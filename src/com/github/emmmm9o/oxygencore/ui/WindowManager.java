package com.github.emmmm9o.oxygencore.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.Core;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Icon;

public class WindowManager extends FloatTable {
  public Cell<Table> menuTableCell;
  public Cell<Table> bodyCell;
  public Table currentMenu;
  public String currentTitle = "Empty";
  public boolean showingMenu;
  public Table settingTable, windowsTable, stylesWindow;
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
    windowsTable.add(windowTable(window, windows_hash))
        .name(Integer.toString(windows_hash)).size(menuSize, 48).uniform().fill().top().grow();
    windows_hash++;
  }

  public void removeWindow(Window window) {
    var index = windows.remove(window);
    var table = windowsTable.find(Integer.toString(index));
    table.visible = false;
    windowsTable.removeChild(table);
    table.remove();
  }

  public void changeVisible(boolean visible, Window window) {
    var index = windows.get(window);
    var table = (Table) windowsTable.find(Integer.toString(index));
    var button = (ImageButton) (((Table) table.find("buttons")).find("visible"));
    button.replaceImage(new Image(visible ? Icon.eye : Icon.eyeOff));
  }

  public void resetStyles(Table table) {
    for (var entry : StyleManager.styles.entrySet()) {
      var style = entry.getValue();
      table.table(style.bodyBackground, one -> {
        one.labelWrap(entry.getKey()).height(48).grow().fill();
        one.button(Icon.pick, style.windowButtons, () -> {
          StyleManager.changeStyle(entry.getKey());
        }).size(48).fill();
      }).size(menuSize, 48).top();
    }
  }
  // public boolean smoothing;
  // public static float moveSmoothFactor = 2f;

  public WindowManager() {
    super();
    windows = new HashMap<>();
    buttons.button(Icon.menu, StyleManager.style.windowButtons, () -> {
      showWindows();
    }).uniform().size(48);
    buttons.button(Icon.settings, StyleManager.style.windowButtons, () -> {
      showSettings();
    }).uniform().size(48);
    buttons.button(Icon.pick, StyleManager.style.windowButtons, () -> {
      showStyles();
    }).uniform().size(48);
    row();
    settingTable = new Table();
    windowsTable = new Table(table -> {

    });
    stylesWindow = new Table(table -> {
      resetStyles(table);
    });
    windowsTable.fillParent = true;
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
      menu.pane(body -> {
        bodyCell = body.table(StyleManager.style.bodyBackground, t -> {
        }).size(menuSize, menuSize - 48).fill();
      }).size(menuSize, menuSize - 48).fill().bottom();
    }).visible(() -> showingMenu && currentMenu != null);
  }

  // TODD Smooth Moving
  /*
   * @Override
   * public void act(float delta) {
   * super.act(delta);
   * if (smoothing) {
   * smoothMoving(delta);
   * }
   * }
   * 
   * public void smoothMoving(float delta) {
   * float target = showingMenu ? menuSize : 0;
   * float now = menuTableCell.get().getHeight();
   * if ((now - target) <= 1f) {
   * menuTableCell.height(target);
   * smoothing = false;
   * return;
   * }
   * Vec2 tmp = new Vec2(menuTableCell.get().getHeight(), 0f);
   * Vec2 tar = new Vec2(target, 0f);
   * tmp = tmp.lerp(tar, moveSmoothFactor * Math.min(0.06f, delta));
   * menuTableCell.height(tmp.x);
   * }
   */
  public void showSettings() {
    ChooseTable(settingTable, "settings");
  }

  public void showWindows() {
    ChooseTable(windowsTable, "windows");
  }

  public void showStyles() {
    ChooseTable(stylesWindow, "styles");
  }

  public void ChooseTable(Table table, String title) {
    if (showingMenu) {
      showMenu();
    } else if (currentTitle == title) {
      showMenu();
    }
    if (currentTitle != title) {
      this.currentMenu = table;
      this.currentTitle = title;
      syncMenu();
      showMenu();
    }
  }

  public void syncMenu() {
    var body = bodyCell.get();
    body.clearChildren();
    body.addChild(this.currentMenu);
  }

  public void showMenu() {
    this.showingMenu = !this.showingMenu;
    if (this.showingMenu) {
      // smoothing = true;
      menuTableCell.size(menuSize);
    } else {
      // smoothing = true;
      menuTableCell.size(menuSize, 0);
    }
  }

  @Override
  public void onTouchUp() {
    Manager.saveManagerPosition();
  }
}
