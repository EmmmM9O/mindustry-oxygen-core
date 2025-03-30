/* (C) 2025 */
package oxygen.ui.dialogs;

import arc.scene.ui.layout.*;
import static oxygen.ui.OCStyles.*;

public class OCSettingsDialog extends OCDialog {
  public Table topContainer, container;

  public OCSettingsDialog() {
    super(ocDialogDraw());
    topContainer = new Table(top -> {
      top.table().grow();
      top.table(buttons -> {
        buttons.defaults().width(70f).height(40f);
        buttons.add(ocCloseButton("@close", this::hide));
      }).height(40f).center();
      top.table().grow();
    });
    add(topContainer).expandX().growX().height(60f).row();
    container = new Table();
    add(container).expandX().grow();
  }
}
