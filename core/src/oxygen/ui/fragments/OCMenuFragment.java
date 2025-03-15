/* (C) 2025 */
package oxygen.ui.fragments;

import arc.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;

import oxygen.graphics.*;
import static oxygen.ui.OCStyles.*;

public class OCMenuFragment extends MenuFragmentI {
  public OCMenuRendererI renderer;
  public Table container;

  @Override
  public void build(Group parent) {
    /*
     * if (renderer == null) renderer = new OCMenuRenderer();
     */
    Group group = new WidgetGroup();
    group.setFillParent(true);
    group.visible(() -> true);
    parent.addChild(group);
    parent = group;
    // Image img = new Image(); img.setFillParent(true); group.addChildAt(0, img);
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

  }

  public void buildButtons() {
    container.clear();
    container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());
    float width = 170f;
    container.left();
    container.add().width(Core.graphics.getWidth() / 30f);
    container.table(t -> {
      t.defaults().width(width).height(40f);
      t.name = "buttons";
      t.add(ocTButton("@play", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@database.button", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@mods", () -> {
      })).marginLeft(11f).row();
      t.add(ocTButton("@settings", () -> {
      })).marginLeft(11f).row();

    }).width(width).growY();
  }
}
