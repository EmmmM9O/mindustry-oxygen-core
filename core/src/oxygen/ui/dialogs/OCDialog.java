package oxygen.ui.dialogs;

import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class OCDialog extends Table {
  public Label title;
  public Table container, titleTable;

  public OCDialog(String title, OCDialogStyle style) {
    this.touchable = Touchable.enabled;
    this.container = new Table();
    add(container).grow().margin(20f).marginLeft(30f).marginRight(30f);
    this.title = new Label(title, style.title);
    titleTable = new Table();
    titleTable.add(this.title).left().expandY();
    titleTable.add().grow();
    container.add(titleTable).growX().row();
    container.add().growX().height(10f);
    setFillParent(true);
  }

  public static class OCDialogStyle {
    public LabelStyle title;
  }
}


