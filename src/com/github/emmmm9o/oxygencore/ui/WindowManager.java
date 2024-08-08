package com.github.emmmm9o.oxygencore.ui;

import java.util.HashMap;
import java.util.Map;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors;

import arc.Core;
import arc.math.Interp;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.scene.ui.ScrollPane;
import arc.util.Align;
import mindustry.core.NetClient;
import mindustry.gen.Icon;

public class WindowManager extends FloatTable {
  public Cell<Table> menuTableCell;
  public Table currentMenu, bodyTable, windowsPaneTable;
  public String currentTitle = "Empty";
  public int currentId = -1;
  public boolean showingMenu;
  public Table windowsTable;
  public int windows_hash = 0;
  public Map<Window, Integer> windows;
  public static float menuSize = 48 * 10;

  public Table windowTable(Window window, int index) {
    var table = new Table();
    table.table(StyleManager.style.titleTextBackground, t -> {
      t.label(() -> table.name).size(48).left().get().setAlignment(Align.center);
    }).size(48).left();
    table.label(() -> window.getTitleConntext()).height(48).grow()
        .get().setAlignment(Align.center);
    table.table(tab -> {
      tab.label(() -> ("x:" + Integer.toString((int) window.x))).height(24).top()
          .grow()
          .get().setAlignment(Align.center);
      tab.row();
      tab.label(() -> ("y:" + Integer.toString((int) window.y))).height(24).bottom()
          .grow()
          .get().setAlignment(Align.center);
    }).grow().height(48);

    table.table(buttons -> {
      buttons
          .button(window.visible ? Icon.eyeSmall : Icon.eyeOffSmall, StyleManager.style.windowButtons, () -> {
            if (window.visible)
              window.hide();
            else
              window.show();
          }).size(48).name("visible");
      buttons.button(Icon.cancel, StyleManager.style.windowButtons, () -> {
        window.close();
      }).size(48);
    }).right().height(48).name("buttons");
    return table;
  }

  public void registerWindow(Window window) {
    windows.put(window, windows_hash);
    windowsPaneTable.add(windowTable(window, windows_hash))
        .name(Integer.toString(windows_hash)).size(menuSize, 48).uniform().top().row();
    windows_hash++;
  }

  public void removeWindow(Window window) {
    var index = windows.remove(window);
    var table = windowsPaneTable.find(Integer.toString(index));
    var cell = windowsPaneTable.getCell(table);
    windowsPaneTable.getCells().remove(cell);
    table.layout();
    table.visible = false;
    windowsPaneTable.removeChild(table);
    table.remove();
  }

  public void changeVisible(boolean visible, Window window) {
    var index = windows.get(window);
    var table = (Table) windowsPaneTable.find(Integer.toString(index));
    var button = (ImageButton) (((Table) table.find("buttons")).find("visible"));
    button.replaceImage(new Image(visible ? Icon.eye : Icon.eyeOff));
  }

  public int default_window_size = 1;
  // public boolean smoothing;
  // public static float moveSmoothFactor = 2f;

  public void newPage (WindowPage page) {
    page.id = pages.size + default_window_size;
    pages.add(page);
  }

  public WindowManager() {
    super();
    var that = this;
    pages = new Seq<>();
    newPage(new WindowPage("@window.settings", Icon.settings) {
      @Override
      public void drawTable() {

      }
    });
    newPage(new WindowPage("@window.styles", Icon.pick) {
      @Override
      public void drawTable() {
        for (var entry : StyleManager.styles.entrySet()) {
          var style = entry.getValue();
          table.add(new Table(style.bodyBackground, one -> {
            String str = entry.getKey();
            one.label(() -> str).height(48).grow().left()
                .get().setAlignment(Align.center);
            one.button(Icon.pick, style.windowButtons, () -> {
              StyleManager.changeStyle(entry.getKey());
            }).size(48).fill().right();
          })).size(menuSize, 48).top().grow().uniform().row();
        }
      }
    });
    newPage(new WindowPage("window.debug", Icon.admin) {
      public TipTable itemSelector;

      @Override
      public void drawTable() {
        itemSelector = Selectors.itemSelector.create(tab -> {
          var dx = that.x   ;
          var dy = that.y-that.getPrefHeight()+48*3;
          return new Vec2(dx, dy);
        }, items -> {
          var str = new StringBuilder();
          for (var it : items) {
            str.append(it.content.localizedName);
            str.append(",");
          }
          NetClient.sendMessage(str.toString());
        }, item -> true);
        itemSelector.visible = false;
        table.button(Icon.add, StyleManager.style.windowButtons, () -> {
          if (itemSelector.visible) {
            itemSelector.hide();
          } else {
            itemSelector.show();
          }
        }).size(48);
      };

      @Override
      public void onClose() {
        itemSelector.close();
      };  

    });
    windows = new HashMap<>();
    buttons.button(Icon.menu, StyleManager.style.windowButtons, () -> {
      showWindows();
    }).uniform().size(48);
    for (var page : pages) {
      page.drawButton(buttons);
    }
    row();
    windowsTable = new Table(table -> {
      windowsPaneTable = new Table(StyleManager.style.bodyBackground);
      table.add(new ScrollPane(windowsPaneTable)).size(menuSize, menuSize - 48).fill().grow();
    });

    menuTableCell = table(menu -> {
      menu.table(StyleManager.style.titleBarBackground, topBar -> {
        topBar.table(StyleManager.style.titleTextBackground, labelBar -> {
          labelBar.label(() -> currentTitle.startsWith("@") ? Core.bundle.get(currentTitle.substring(1)) : currentTitle)
              .fill().growX().height(48).get().setAlignment(Align.center);
        }).left().height(48).growX().fill();
        topBar.table(buttonsBar -> {
          buttonsBar.button(Icon.cancel, StyleManager.style.windowButtons, () -> {
            showMenu();
            
          }).size(48);
        }).right().height(48).fill();
      }).size(menuSize, 48).fill().top();
      menu.row();
      bodyTable = new Table(StyleManager.style.bodyBackground);
      menu.add(bodyTable).size(menuSize, menuSize - 48).fill().bottom().grow();
    }).visible(() -> showingMenu && currentMenu != null);
  }

  public void showWindows() {
    ChooseTable(windowsTable, "@window.windows", 0);
  }

  public void ChooseTable(Table table, String title, int id) {

    if (showingMenu) {
      showMenu();
    } else if (currentTitle == title) {
      showMenu();
    }
    if (currentTitle != title) {
      this.currentId = id;
      this.currentMenu = table;
      this.currentTitle = title;
      syncMenu();
      showMenu();
    }
  }

  public void syncMenu() {
    bodyTable.clearChildren();
    bodyTable.add(this.currentMenu).size(menuSize, menuSize - 48).fill().uniform().grow();
    // menuTableCell.get().pack();
  }

  public void showMenu() {
    this.showingMenu = !this.showingMenu;
    menuTableCell.get().setTransform(true);
    if (this.showingMenu) {
      // smoothing = true;
      if (currentId >= default_window_size) {
        pages.get(currentId - default_window_size).onShow();
      }
      menuTableCell.size(menuSize);
      menuTableCell.get().actions(Actions.alpha(0f), Actions.fadeIn(0.1f, Interp.fade));
    } else {
      if (currentId >= default_window_size) {
        pages.get(currentId - default_window_size).onClose();
      }
      // smoothing = true;
      menuTableCell.size(menuSize, 0);
      menuTableCell.get().actions(Actions.alpha(1f), Actions.fadeIn(0.1f, Interp.fade));
    }
  }

  @Override
  public void onTouchUp() {
    Manager.saveManagerPosition();
  }

  public class WindowPage {
    public String title;
    public int id;
    public Table table;
    public Drawable icon;

    public WindowPage(String title, Drawable icon) {
      this.title = title;
      this.icon = icon;
      table = new Table();
      drawTable();
    }

    public void drawTable() {
    }

    public void jumpTo() {
      ChooseTable(table, title, id);
    }

    public void onClose() {

    }

    public void onShow() {

    }

    public void drawButton(Table table) {
      buttons.button(icon, StyleManager.style.windowButtons, () -> {
        jumpTo();
      }).uniform().size(48);
    }

  }

  public Seq<WindowPage> pages;
}
