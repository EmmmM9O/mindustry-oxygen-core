/* (C) 2025 */
package oxygen.ui.fragments;

import arc.*;
import arc.math.*;
import arc.util.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.core.*;
import mindustry.ui.fragments.*;
import mindustry.graphics.*;
import oxygen.core.OCMain;
import oxygen.graphics.*;

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
    Image img = new Image(Core.atlas.drawable(OCMain.name + "-background"));
    img.setFillParent(true);
    group.addChildAt(0, img);
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
    String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
    parent.fill((x, y, w, h) -> {
      TextureRegion logo = Core.atlas.find("logo");
      float width = Core.graphics.getWidth(),
          height = Core.graphics.getHeight() - Core.scene.marginTop;
      float logoscl = Scl.scl(0.6f) * logo.scale;
      float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
      float logoh = logow * (float) logo.height / logo.width;

      float fx = logow/2+10f;
      float fy =
          (int) (height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f)-90f;

      Draw.color();
      Draw.rect(logo, fx, fy, logow, logoh);

      Fonts.outline.setColor(Color.white);
      Fonts.outline.draw(versionText, fx, fy - logoh / 2f - Scl.scl(2f), Align.center);
    }).touchable = Touchable.disabled;
  }

  public void buildButtons() {
    container.clear();
    container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());
    float width = 230f;
    Drawable background = Styles.black6;
    container.left();
    container.add().width(/*Core.graphics.getWidth() / 10f*/10f);
    container.table(background, t -> {
      t.defaults().width(width).height(70f);
      t.name = "buttons";
      t.button("@play", Icon.play, Styles.flatToggleMenut, () -> {
      }).marginLeft(11f).row();
      t.button("@database.button", Icon.menu, Styles.flatToggleMenut, () -> {
      }).marginLeft(11f).row();
      t.button("@mods", Icon.book, Styles.flatToggleMenut, () -> {
      }).marginLeft(11f).row();
      t.button("@settings", Icon.settings, Styles.flatToggleMenut, () -> {
      }).marginLeft(11f).row();
    }).width(width).growY();
    container.table(background, t -> {
      t.name = "submenu";
      t.color.a = 0f;
      t.top();
      t.defaults().width(width).height(70f);
      t.visible(() -> !t.getChildren().isEmpty());
    }).width(width).growY();
  }
}
