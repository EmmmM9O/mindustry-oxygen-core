package oxygen.ui.fragments;

import arc.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;
import oxygen.graphics.*;

public class OCMenuFragment extends MenuFragmentI {
  public OCMenuRendererI renderer;
  public Table container;

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
      });
    });
  }

  public void buildButtons() {
    container.clear();
    container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());
    float width = 230f;
    Drawable background = Styles.black6;
    container.left();
    container.add().width(Core.graphics.getWidth()/10f);
    container.table(background, t -> {
      t.defaults().width(width).height(70f);
      t.name = "buttons";
    })width(width).growY();
  }
}
