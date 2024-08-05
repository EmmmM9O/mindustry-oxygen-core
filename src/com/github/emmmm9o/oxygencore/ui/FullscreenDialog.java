
package com.github.emmmm9o.oxygencore.ui;

import arc.Core;
import arc.scene.ui.Dialog;
import arc.util.Align;
import mindustry.gen.Icon;

public class FullscreenDialog extends Dialog {
  public Window window;

  public FullscreenDialog(Window window) {
    super(window.getTitleConntext(), StyleManager.style.fullscreenDialogStyle);
    this.window = window;
    this.title.setText(() -> this.window.getTitleConntext());
    titleTable.removeChild(this.title);
    titleTable.clearChildren();
    titleTable.table(tab -> {
      tab.add(this.title).left().grow().get().setAlignment(Align.center);
    }).grow().left().get().setBackground(StyleManager.style.titleTextBackground);
    titleTable.table(rightBar -> {
      rightBar
          .button(Core.atlas.drawable("oxygen-core-consume"), StyleManager.style.windowButtons,
              () -> {
                window.fullscreen();
                this.hide();
              })
          .uniform().right().grow().size(60f);
      rightBar
          .button(Core.atlas.drawable("oxygen-core-hide"), StyleManager.style.windowButtons,
              () -> {
                window.hide();
                this.hide();
              })
          .uniform().right().grow().size(60f);
      rightBar
          .button(Icon.cancel, StyleManager.style.windowButtons, (() -> {
            window.close();
            this.hide();
          })).uniform().right().grow().size(60f);
    }).right().uniformY().growY();
    setFillParent(true);
    titleTable.setBackground(StyleManager.style.titleBarBackground);
    cont.setBackground(StyleManager.style.bodyBackground);
    /*
     * cont.table(table -> {
     * window.drawBody(table);
     * });
     */
    buttons.setBackground(StyleManager.style.statusBarBackground);
    /*
     * buttons.table(table -> {
     * window.drawStatus(table);
     * });
     */
  }

  @Override
  public void addCloseButton() {

  }
}
