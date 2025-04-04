/* (C) 2025 */
package oxygen.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;

import oxygen.graphics.*;
import oxygen.graphics.universe.*;

import static oxygen.ui.OCStyles.*;
import static oxygen.graphics.OCShaders.*;
import static oxygen.ui.OCUI.*;

public class OCMenuFragment extends MenuFragmentI {
  public OCMenuRendererI renderer;
  public Table container, buttonContainer;
  public float menuButtonHeight = 40f, menuButtonWidth = 180f;
  public float logoScl = 0.25f;

  @Override
  public void build(Group parent) {
    if (renderer == null)
      renderer = new OCMenuRenderer();
    Group group = new WidgetGroup();
    group.setFillParent(true);
    group.visible(() -> true);
    parent.addChild(group);
    parent = group;
    parent.fill((x, y, w, h) -> {

    });
    parent.addChild(new Element() {
      @Override
      public void draw() {
        if (renderer != null)
          renderer.render();
      };
    });

    parent.fill(c -> {
      c.left();
      c.pane(Styles.noBarPane, cont -> {
        container = cont;
        cont.name = "menu container";
        buildButtons();
        Events.on(ResizeEvent.class, event -> buildButtons());
      }).with(pane -> {
        pane.setOverscroll(false, false);
      }).grow();
    });
    parent.fill(info -> {
      info.right().top();
      IntFormat fps = new IntFormat("fps");
      info.label(() -> fps.get(Core.graphics.getFramesPerSecond())).left()
          .style(Styles.outlineLabel).name("fps");
    });
    // String versionText =
    // ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
    /*
     * parent.fill((x, y, w, h) -> { TextureRegion logo = Core.atlas.find("logo"); float height =
     * Core.graphics.getHeight() - Core.scene.marginTop; float topX = (height - buttonListHeight())
     * / 2 + buttonListHeight(); float logoh = (height - topX) * logoScl; float logow = logoh /
     * logo.height * logo.width; float fx = logow / 2 + 40f; float fy = height / 2 + topX / 2 + 40f;
     * Draw.color(); Draw.rect(logo, fx, fy, logow, logoh); Fonts.outline.setColor(Color.orange);
     * Fonts.outline.draw(versionText, fx, fy - logoh / 2f - Scl.scl(2f), Align.center);
     * }).touchable = Touchable.disabled;
     */
    parent.fill(t -> {
      t.right().bottom();
      t.table(tab -> {
        tab.add("bloomIterations");
        tab.slider(1.0f, 8.0f, 1.0f, BlackHoleRenderer.bloomIterations * 1.0f, r -> {
          BlackHoleRenderer.bloomIterations = (int) r;
        }).row();
        tab.add("gamma");
        tab.slider(1.0f, 4.0f, 0.1f, tonemapping.gamma, r -> {
          tonemapping.gamma = r;
        }).row();
        tab.add("bloom_strength");
        tab.slider(0.0f, 4.0f, 0.1f, bloomComposite.bloom_strength, r -> {
          bloomComposite.bloom_strength = r;
        }).row();
        tab.add("noise_LOD");
        tab.slider(1.0f, 8.0f, 1.0f, blackhole.adiskNoiseLOD * 1.0f, r -> {
          blackhole.adiskNoiseLOD = (int) r;
        }).row();
        tab.add("horizon_radius");
        tab.slider(0.2f, 4.0f, 0.1f, blackhole.horizonRadius, r -> {
          blackhole.horizonRadius = r;
        }).row();
        tab.add("max_steps");
        tab.slider(150f, 300f, 10f, blackhole.maxSteps, r -> {
          blackhole.maxSteps = (int) r;
        }).row();
        tab.add("max_length");
        tab.slider(15f, 40f, 2.5f, blackhole.maxLength, r -> {
          blackhole.maxLength = r;
        }).row();
        tab.add("fov_scl");
        tab.slider(0.2f, 4.0f, 0.1f, blackhole.horizonRadius, r -> {
          blackhole.horizonRadius = r;
        }).row();
      });
      t.table(tab -> {
        tab.add("height");
        tab.slider(0.1f, 2.0f, 0.05f, blackhole.adiskHeight, r -> {
          blackhole.adiskHeight = r;
        }).row();
        tab.add("lit");
        tab.slider(0.00f, 1.0f, 0.01f, blackhole.adiskLit, r -> {
          blackhole.adiskLit = r;
        }).row();
        tab.add("particle");
        tab.slider(0.2f, 1.5f, 0.1f, blackhole.adiskParticle, r -> {
          blackhole.adiskParticle = r;
        }).row();
        tab.add("noise_scale");
        tab.slider(0.1f, 3f, 0.05f, blackhole.adiskNoiseScale, r -> {
          blackhole.adiskNoiseScale = r;
        }).row();
        tab.add("adiskDensityV");
        tab.slider(1f, 10f, 0.5f, blackhole.adiskDensityV, r -> {
          blackhole.adiskDensityV = r;
        }).row();
        tab.add("adiskDensityH");
        tab.slider(1f, 10f, 0.5f, blackhole.adiskDensityH, r -> {
          blackhole.adiskDensityH = r;
        }).row();
        tab.add("adiskOuterRadius");
        tab.slider(3f, 20f, 1f, blackhole.adiskOuterRadius, r -> {
          blackhole.adiskOuterRadius = r;
        }).row();
      });
    });
  }

  public float buttonListHeight() {
    return 10 * menuButtonHeight;
  }

  public void buildButtons() {
    container.clear();
    container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());
    container.left();
    container.add().width(Core.graphics.getWidth() / 30f);
    container.table(t -> {
      buttonContainer = t;
      t.defaults().width(menuButtonWidth).height(menuButtonHeight);
      t.name = "buttons";
      t.add(ocTButton("@oxygen.play", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.continue", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.load", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.mods", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.database", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.settings", settingsDialog::show)).marginLeft(11f).row();
      t.add(ocTButton("@oxygen.exit", () -> {
      })).marginLeft(11f).row();
    }).width(menuButtonWidth).growY();
  }
}
