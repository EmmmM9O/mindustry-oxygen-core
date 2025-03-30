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
import static oxygen.ui.OCStyles.*;
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
      if (renderer != null)
        renderer.render();
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
    String versionText =
        ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
    parent.fill((x, y, w, h) -> {
      TextureRegion logo = Core.atlas.find("logo");
      float height = Core.graphics.getHeight() - Core.scene.marginTop;
      float topX = (height - buttonListHeight()) / 2 + buttonListHeight();
      float logoh = (height - topX) * logoScl;
      float logow = logoh / logo.height * logo.width;
      float fx = logow / 2 + 40f;
      float fy = height / 2 + topX / 2 + 40f;
      Draw.color();
      Draw.rect(logo, fx, fy, logow, logoh);
      Fonts.outline.setColor(Color.orange);
      Fonts.outline.draw(versionText, fx, fy - logoh / 2f - Scl.scl(2f), Align.center);
    }).touchable = Touchable.disabled;
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
