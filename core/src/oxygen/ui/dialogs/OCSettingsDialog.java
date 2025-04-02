/* (C) 2025 */
package oxygen.ui.dialogs;

import arc.scene.ui.layout.*;
import static oxygen.ui.OCStyles.*;

public class OCSettingsDialog extends OCDialog {
  public Table topContainer, container, menu;

  public OCSettingsDialog() {
    super(ocDialogDraw());
    topContainer = new Table(top -> {
      top.table().grow().expandX().row();
      top.table(buttons -> {
        buttons.defaults().width(70f).height(40f);
        buttons.add(ocCloseButton("@close", this::hide)).left().marginLeft(18f);
        buttons.add().grow();
      }).height(40f).center().expandX().growX().row();
      top.table().expandX().grow();
    });
    add(topContainer).expandX().growX().height(60f).row();
    add().growX().height(20f).row();
    container = new Table(oacid3, con -> {
      menu = new Table(oacid3);
      buildMenu();
      con.add(menu).growY().width(160f);
      con.add().grow();
    });
    add(container).expandX().grow().row();
    add().growX().height(25f).row();
  }

  public void buildMenu() {
    menu.clearChildren();
    menu.pane(tab -> {

    }).grow();
  }
}
