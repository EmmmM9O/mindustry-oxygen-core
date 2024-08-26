package com.github.emmmm9o.oxygencore.ui.dialogs;

import arc.Core;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.graphics.g3d.PlanetRenderer;
import mindustry.graphics.g3d.PlanetRenderer.PlanetInterfaceRenderer;
import mindustry.type.Planet;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import static mindustry.Vars.*;

import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.ui.layout.TreeTable;

import static arc.Core.*;

/**
 * OxygenPlanetDialog
 */
public class OxygenPlanetDialog extends BaseDialog implements PlanetInterfaceRenderer {
  public PlanetParams state = new PlanetParams();
  public float zoom = 1f;
  public TreeTable<Planet> planetsTree;
  public String searchText = "";
  public Label hoverLabel = new Label("");
  public Table sectorTop = new Table(), notifs = new Table(), expandTable = new Table();
  public Mode mode = Mode.look;
  public final PlanetRenderer planets = renderer.planets;

  public OxygenPlanetDialog() {
    super("", Styles.fullDialog);
    state.renderer = this;
    state.drawUi = true;

    shouldPause = true;
    state.planet = Planets.serpulo;
    hoverLabel.setStyle(Styles.outlineLabel);
    hoverLabel.setAlignment(Align.center);
    planetsTree = new TreeTable<>(Vars.content.planets(), planet -> planet.parent) {
      public void drawNodeContent(Planet data, Table table) {
        var ta = table.table(text -> {
          text.add(data.localizedName).grow().get().setAlignment(Align.center);
        }).grow().height(StyleManager.ButtonSize).get();
        ta.touchable = Touchable.enabled;
        ta.addListener(new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            if (state.planet != data) {
              state.planet = data;
              rebuildExpand();
            }
            return true;
          }
        });
      };
    };

    rebuildButtons();

    onResize(this::rebuildButtons);
    dragged((cx, cy) -> {
      if (Core.input.getTouches() > 1)
        return;
      Vec3 pos = state.camPos;
      float upV = pos.angle(Vec3.Y);
      float xscale = 9f, yscale = 10f;
      float margin = 1;
      float speed = 1f - Math.abs(upV - 90) / 90f;

      pos.rotate(state.camUp, cx / xscale * speed);
      float amount = cy / yscale;
      amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

      pos.rotate(Tmp.v31.set(state.camUp).rotate(state.camDir, 90), amount);
    });
    addListener(new InputListener() {
      @Override
      public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
        if (event.targetActor == OxygenPlanetDialog.this) {
          zoom = Mathf.clamp(zoom + amountY / 10f, state.planet.minZoom, 2f);
        }
        return true;
      }
    });
    addCaptureListener(new ElementGestureListener() {
      float lastZoom = -1f;

      @Override
      public void zoom(InputEvent event, float initialDistance, float distance) {
        if (lastZoom < 0) {
          lastZoom = zoom;
        }

        zoom = (Mathf.clamp(initialDistance / distance * lastZoom, state.planet.minZoom, 2f));
      }

      @Override
      public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
        lastZoom = zoom;
      }
    });
    shown(this::setup);
  }

  @Override
  public void renderSectors(Planet planet) {

  }

  @Override
  public void renderProjections(Planet planet) {

  }

  void rebuildButtons() {
    buttons.clearChildren();

    buttons.bottom();
    if (Core.graphics.isPortrait()) {
      buttons.add(sectorTop).colspan(2).fillX().row();
      addBack();
      addInfo();
    } else {
      addBack();
      buttons.add().growX();
      buttons.add(sectorTop).minWidth(230f);
      buttons.add().growX();
      addInfo();
    }
  }

  void addBack() {
    buttons.button("@back", Icon.left, this::hide).size(200f, 54f).pad(2).bottom();
  }

  void addInfo() {
    buttons.button("@info", Icon.info, () -> {
    }).size(200f, 54f).pad(2).bottom();
  }

  boolean showing() {
    return false;
  }

  void setup() {
    searchText = "";
    zoom = state.zoom = 1f;
    state.uiAlpha = 1f;
    ui.minimapfrag.hide();

    clearChildren();

    margin(0f);
    stack(
        new Element() {
          {

          }

          @Override
          public void act(float delta) {
            if (scene.getDialog() == OxygenPlanetDialog.this
                && !scene.hit(input.mouseX(), input.mouseY(), true).isDescendantOf(e -> e instanceof ScrollPane)) {
              scene.setScrollFocus(OxygenPlanetDialog.this);
            }
            super.act(delta);
          }

          @Override
          public void draw() {
            planets.render(state);
          }

        },
        new Table(t -> {
          t.touchable = Touchable.disabled;
          t.top();
          t.label(() -> mode == Mode.select ? "@sectors.select" : "").style(Styles.outlineLabel).color(Pal.accent);
        }),
        buttons,
        new Table(StyleManager.style.bodyBackground, t -> {
          t.top().left();
          t.add(planetsTree).top().left();
        }),
        new Table(c -> {
          expandTable = c;
        })).grow();
    rebuildExpand();
  }

  void rebuildExpand() {
    Table c = expandTable;
    c.clear();
    c.visible(() -> !(graphics.isPortrait() && mobile));

  }

  @Override
  public Dialog show() {
    if (net.client()) {
      ui.showInfo("@map.multiplayer");
      return this;
    }
    if (Vars.state.rules.sector != null) {
      state.planet = Vars.state.rules.sector.planet;
      settings.put("lastplanet", state.planet.name);
    }
    rebuildButtons();
    mode = Mode.look;
    state.otherCamPos = null;
    zoom = 1f;
    state.zoom = 1f;
    state.uiAlpha = 0f;
    return super.show();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (state.otherCamPos != null) {
      state.otherCamAlpha = Mathf.lerpDelta(state.otherCamAlpha, 1f, 0.05f);
      state.camPos.set(0f, PlanetRenderer.camLength, 0.1f);
      if (Mathf.equal(state.otherCamAlpha, 1f, 0.01f)) {
        state.camPos.set(Tmp.v31.set(state.otherCamPos).lerp(state.planet.position, state.otherCamAlpha)
            .add(state.camPos).sub(state.planet.position));

        state.otherCamPos = null;
      }
    }
    state.zoom = Mathf.lerpDelta(state.zoom, zoom, 0.4f);
    state.uiAlpha = Mathf.lerpDelta(state.uiAlpha, Mathf.num(state.zoom < 1.9f), 0.1f);
  }

  public enum Mode {
    look,
    select,
    planetLaunch
  }

}
