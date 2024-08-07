package com.github.emmmm9o.oxygencore.ui;

import java.util.HashMap;
import java.util.Map;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors;

import arc.Core;
import arc.math.Interp;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ScrollPane;
import arc.util.Align;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.gen.Icon;

public class WindowManager extends FloatTable {
  public Cell<Table> menuTableCell;
  public Table currentMenu, bodyTable, windowsPaneTable;
  public String currentTitle = "Empty";
  public boolean showingMenu;
  public Table settingTable, windowsTable, stylesWindow, debugWindow;
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

  public void resetStyles(Table table) {
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

  // public boolean smoothing;
  // public static float moveSmoothFactor = 2f;
  public TipTable itemSelector;

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
    buttons.button(Icon.admin, StyleManager.style.windowButtons, () -> {
      showDebug();
    }).uniform().size(48);
    row();
    settingTable = new Table();
    windowsTable = new Table(table -> {
      windowsPaneTable = new Table(StyleManager.style.bodyBackground);
      table.add(new ScrollPane(windowsPaneTable)).size(menuSize, menuSize - 48).fill().grow();
    });
    stylesWindow = new Table(table -> {
    });
    debugWindow = new Table(table -> {
      itemSelector = Selectors.itemSelector.create(() -> {
        var dx = this.x + this.getWidth();
        var dy = this.y;
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
    });
    resetStyles(stylesWindow);
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
    ChooseTable(settingTable, "@window.settings");
  }

  public void showWindows() {
    ChooseTable(windowsTable, "@window.windows");
  }

  public void showStyles() {
    ChooseTable(stylesWindow, "@window.styles");
  }

  public void showDebug() {
    ChooseTable(debugWindow, "window.debug");
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
    bodyTable.clearChildren();
    bodyTable.add(this.currentMenu).size(menuSize, menuSize - 48).fill().uniform().grow();
    // menuTableCell.get().pack();
  }

  public void showMenu() {
    this.showingMenu = !this.showingMenu;
    menuTableCell.get().setTransform(true);
    if (this.showingMenu) {
      // smoothing = true;

      menuTableCell.size(menuSize);
      menuTableCell.get().actions(Actions.alpha(0f), Actions.fadeIn(0.1f, Interp.fade));
    } else {
      // smoothing = true;
      menuTableCell.size(menuSize, 0);
      menuTableCell.get().actions(Actions.alpha(1f), Actions.fadeIn(0.1f, Interp.fade));
    }
  }

  @Override
  public void onTouchUp() {
    Manager.saveManagerPosition();
  }
}
