package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.serialization.Base64Coder;
import mindustry.gen.Icon;

/**
 * IOPortBlockWindow
 */
public class IOPortBlockWindow extends BlockWindow {
  public IOBuild ioBuild;

  public IOPortBlockWindow(IOBuild ioBuild) {
    super(ioBuild);
    this.ioBuild = ioBuild;
  }

  public void drawSelect(Table tab, boolean row, boolean del, int start) {
    for (int d = 0; d < ioBuild.block.size; d++) {
      var port = ioBuild.ports.get(del ? start + ioBuild.block.size - 1 - d : start + d);
      var k=new int[1];
      k[0]=del ? start + ioBuild.block.size - 1 - d : start + d;
      tab.table(StyleManager.style.bodyBackground,
          t -> {
            //t.add(Integer.toString(k[0]));
            
             if (port == null) {
             t.image(Icon.add).size(StyleManager.XButtonSize);
             } else {
              port.display(t);
              }
             
          }).size(StyleManager.XButtonSize);
      if (row) {
        tab.row();
      }
    }
  }

  @Override
  public void drawControlTable(Table table) {
    table.table(cont -> {
      cont.table(top -> {
        top.table().size(StyleManager.XButtonSize).left();
        top.table(tab -> {
          drawSelect(tab, false, true, ioBuild.block.size);
        }).size(StyleManager.XButtonSize * ioBuild.block.size, StyleManager.XButtonSize).left();
        top.table().size(StyleManager.XButtonSize).left();
      })
          .size(StyleManager.XButtonSize * (ioBuild.block.size + 2), StyleManager.XButtonSize).left();
      cont.row();
      cont.table(ct->{
      ct.table(left -> {
        drawSelect(left, true, false, ioBuild.block.size * 2);
      }).size(StyleManager.XButtonSize, StyleManager.XButtonSize * ioBuild.block.size).left();
      ct.table(StyleManager.style.bodyBackground, center -> {
        center.image(ioBuild.block.fullIcon).size(StyleManager.XButtonSize * ioBuild.block.size).grow();
      }).size(StyleManager.XButtonSize * ioBuild.block.size).left();
      ct.table(right -> {
        drawSelect(right, true, true, 0);
      }).size(StyleManager.XButtonSize, StyleManager.XButtonSize * ioBuild.block.size);
      }).size(StyleManager.XButtonSize*(ioBuild.block.size+2), StyleManager.XButtonSize * ioBuild.block.size);
      cont.row();
      cont.table(bottom -> {
        bottom.table().size(StyleManager.XButtonSize).left();
        bottom.table(tab -> {
          drawSelect(tab, false, false, ioBuild.block.size * 3);
        }).size(StyleManager.XButtonSize * ioBuild.block.size, StyleManager.XButtonSize).left();
        bottom.table().size(StyleManager.XButtonSize).left();
      })
          .size(StyleManager.XButtonSize * (ioBuild.block.size + 2), StyleManager.XButtonSize);
    }).size(StyleManager.XButtonSize * (ioBuild.block.size + 2));
  }

  public String copy() {
    return new String(Base64Coder.encode(ioBuild.config()));
  }

  public void paste(String str) {
    ioBuild.configure(Base64Coder.decode(str));
  }

  public void clearS() {
    ioBuild.clearS();
  }

  @Override
  public void drawStatus(Table tab) {
    tab.table(buttons -> {
      buttons.button(Icon.trash, StyleManager.style.windowButtons, () -> {
        clearS();
      }).size(48);
      buttons.button(Icon.copy, StyleManager.style.windowButtons, () -> {
        Core.app.setClipboardText(copy());
      }).size(48);
      buttons.button(Icon.paste, StyleManager.style.windowButtons, () -> {
        paste(Core.app.getClipboardText());
      }).size(48);
    }).right().height(48);
  }
}
