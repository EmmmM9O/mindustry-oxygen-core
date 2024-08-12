package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.blocks.BasicWindowBlock.BasicWindowBuild;
import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.ui.selectors.SelectorType;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors;

import arc.Core;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.serialization.Base64Coder;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

import com.github.emmmm9o.oxygencore.ui.selectors.Selectors.PortSelectorable;
import arc.func.Func;
import com.github.emmmm9o.oxygencore.io.IOPortType;

/**
 * IOPortBlockWindow
 */
public class IOPortBlockWindow extends BlockWindow {
  public IOBuild ioBuild;
  public int currentID = -1;
  public SelectorType<PortSelectorable, Func<IOPortType, Boolean>>.Selector selectorTable;
  public Seq<Table> lists;
  public TipTable configureTable;

  @Override
  public void hide() {
    super.hide();
    selectorTable.hide();
    configureTable.hide();
    currentID = -1;
  }

  @Override
  public void close() {
    super.close();
    selectorTable.close();
    configureTable.close();
    lists.clear();
    currentID = -1;
  }

  public void showTip() {
    if (!selectorTable.visible)
      selectorTable.show();
  }

  public Table getById(int id) {
    for (var t : lists) {
      var k = ((Table) t.find(Integer.toString(id)));
      if (k != null)
        return k;
    }
    return null;
  }

  public void clearAllHightlight() {
    for (var t : lists) {
      var k = ((Table) t.find(Integer.toString(currentID)));
      if (k != null)
        k.setBackground(StyleManager.style.bodyBackground);
    }
  }

  public void syncConfigureTable() {
    configureTable.clearChildren();
    if (currentID == -1) {
      configureTable.hide();
    } else {
      var port = ioBuild.ports.get(currentID);
      configureTable.table(main -> {
        main.table(StyleManager.style.titleBarBackground, top -> {
          top.add(port == null ? "none" : port.type.localizedName)
              .height(StyleManager.ButtonSize).left().grow().get().setAlignment(Align.center);
          top.add("id:" + currentID)
              .height(StyleManager.ButtonSize).left().grow().get().setAlignment(Align.center);
        }).height(StyleManager.ButtonSize).growX().uniformX().row();
        main.table(StyleManager.style.bodyBackground, cont -> {
          cont.table(type -> {
            if (ioBuild.ports.get(currentID) != null)
              ioBuild.ports.get(currentID).type.display(type);
          }).grow().uniformX().row();
          cont.table(info -> {
            if (ioBuild.ports.get(currentID) != null)
              ioBuild.ports.get(currentID).infoDisplay(info);
          }).grow().uniformX().row();
          cont.table(configure -> {
            if (ioBuild.ports.get(currentID) != null)
              ioBuild.ports.get(currentID).configureDisplay(configure);
          }).grow().uniformX();
        }).grow().uniformX();
      }).grow();
    }
  }

  public IOPortBlockWindow(IOBuild ioBuild) {
    super(ioBuild);
    this.ioBuild = ioBuild;
    lists = new Seq<>();
    configureTable = new TipTable(tab -> {
      var dx = this.x + this.getWidth() + tab.getPrefWidth() / 2 + 36f;
      var dy = this.y + this.getHeight() - 24f;
      return new Vec2(dx, dy);
    }, StyleManager.style.bodyBackground);
    configureTable.visible = false;

    selectorTable = Selectors.radioPortSelector.create_radio(tab -> {
      var dx = this.x + this.getWidth() / 2;
      var dy = this.y - this.getHeight() / 2 + tab.getHeight() + 52f;
      return new Vec2(dx, dy);
    }, portS -> {
      var t = getById(currentID);
      t.clearChildren();
      if (portS != null) {
        var portType = portS.content;
        var port = portType.create(ioBuild, currentID);
        ioBuild.changePort(currentID, port);
        clearAllHightlight();
        port.display(t);
      } else {
        ioBuild.changePort(currentID, null);
        clearAllHightlight();
        t.image(Icon.add).size(StyleManager.XButtonSize);
      }
      configureTable.close();
      currentID = -1;
    }, p -> true);
    selectorTable.visible = false;
  }

  public void drawSelect(Table tab, boolean row, boolean del, int start) {
    lists.add(tab);
    for (int d = 0; d < ioBuild.block.size; d++) {
      var id = del ? start + ioBuild.block.size - 1 - d : start + d;
      var port = ioBuild.ports.get(id);
      tab.table(StyleManager.style.bodyBackground,
          t -> {
            if (port == null) {
              t.image(Icon.add).size(StyleManager.XButtonSize);
            } else {
              port.display(t);
            }
          }).size(StyleManager.XButtonSize)
          .name(Integer.toString(id)).get().addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
              if (currentID != id) {
                clearAllHightlight();
                ((Table) tab.find(Integer.toString(id))).setBackground(Styles.accentDrawable);
                if (currentID != -1 && ((selectorTable.selected == null && ioBuild.ports.get(currentID) != null)
                    || (ioBuild.ports.get(currentID) == null && selectorTable.selected != null)
                    || (((Selectors.PortSelectorable) selectorTable.selected.get(0)).content != ioBuild.ports
                        .get(currentID).type)))
                  selectorTable.callback.get(selectorTable.selected);
                selectorTable.clearS();
                if (ioBuild.ports.get(id) != null) {
                  selectorTable.select(selectorTable.list.find(l -> l.content == ioBuild.ports.get(id).type));
                }
                currentID = id;
                configureTable.show();
                syncConfigureTable();
                showTip();
              } else {
                selectorTable.hide();
              }
              return true;
            }
          });
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
      cont.table(ct -> {
        ct.table(left -> {
          drawSelect(left, true, false, ioBuild.block.size * 2);
        }).size(StyleManager.XButtonSize, StyleManager.XButtonSize * ioBuild.block.size).left();
        ct.table(StyleManager.style.bodyBackground, center -> {
          center.image(ioBuild.block.fullIcon).size(StyleManager.XButtonSize * ioBuild.block.size).grow();
        }).size(StyleManager.XButtonSize * ioBuild.block.size).left();
        ct.table(right -> {
          drawSelect(right, true, true, 0);
        }).size(StyleManager.XButtonSize, StyleManager.XButtonSize * ioBuild.block.size);
      }).size(StyleManager.XButtonSize * (ioBuild.block.size + 2), StyleManager.XButtonSize * ioBuild.block.size);
      cont.row();
      cont.table(bottom -> {
        bottom.table().size(StyleManager.XButtonSize).left();
        bottom.table(tab -> {
          drawSelect(tab, false, false, ioBuild.block.size * 3);
        }).size(StyleManager.XButtonSize * ioBuild.block.size, StyleManager.XButtonSize).left();
        bottom.table().size(StyleManager.XButtonSize).left();
      })
          .size(StyleManager.XButtonSize * (ioBuild.block.size + 2), StyleManager.XButtonSize);
    }).size(StyleManager.XButtonSize * (ioBuild.block.size + 2)).row();
    if (building instanceof BasicWindowBuild bwb) {
      bwb.displayWindowExtra(table);
    }
  }

  public String copy() {
    return new String(Base64Coder.encode(ioBuild.config()));
  }

  public void paste(String str) {
    try {
      if (str.length() <= 8)
        return;
      ioBuild.configure(Base64Coder.decode(str));
    } catch (Throwable err) {
    }
  }

  public void clearS() {
    ioBuild.clearS();
  }

  @Override
  public void drawStatus(Table tab) {
    tab.table(text -> {
      text.label(() -> currentID == -1 ? "un selected" : Integer.toString(currentID))
          .height(StyleManager.ButtonSize).grow().get().setAlignment(Align.center);
    }).height(StyleManager.ButtonSize).grow();
    tab.table(buttons -> {
      buttons.button(Icon.trash, StyleManager.style.windowButtons, () -> {
        clearS();
      }).size(StyleManager.ButtonSize);
      buttons.button(Icon.copy, StyleManager.style.windowButtons, () -> {
        Core.app.setClipboardText(copy());
      }).size(StyleManager.ButtonSize);
      buttons.button(Icon.paste, StyleManager.style.windowButtons, () -> {
        paste(Core.app.getClipboardText());
        close();
      }).size(StyleManager.ButtonSize);
    }).right().height(StyleManager.ButtonSize);
  }
}
