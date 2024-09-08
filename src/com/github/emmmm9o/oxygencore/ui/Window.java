package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.Core;
import arc.func.Func;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.gen.Icon;

public class Window {
  public String title = "Empty";
  public Func<Window, String> titleFunc = null;
  public Table table, bodyContainer, statusBarContainer;
  public float x, y, startX = -1, startY = -1;
  public float width = 100, height = 100;
  public boolean resizeable = true, draggable = true, moving, resizing;
  public boolean visible, fullscreen, center, added, closed;
  public static float draggedAlpha = 0.45f;
  protected Cell<Table> titleBarCell, statusBarCell;
  protected Cell<Table> titleTextBarCell;

  protected Cell<Table> bodyCell;

  protected Cell<ImageButton> resizeButtonCell;
  public ScrollPane bodyScroll;

  public void setStart(float x, float y) {
    this.startX = x;
    this.startY = y;
  }

  public boolean active() {
    return Manager.activeWindow == this;
  }

  public void onClose() {

  }

  public void onHide() {

  }

  public void onShow() {

  }

  public void onMove(float tx, float ty) {

  }

  public void onResize(float wi, float hi) {

  }

  public void resize(float dwidth, float dheight) {
    width = dwidth;
    height = dheight;
    width = Math.max(width, 96f);
    height = Math.max(height, 64f);

    layout();
  }

  public void layout(float width, float height) {
    table.setSize(width, height + StyleManager.ButtonSize);
    titleBarCell.get().setWidth(width);
    titleTextBarCell.get().setWidth(width - 32f * (4 + (resizeable ? 1 : 0)));
    this.bodyCell.size(width, height);
    this.statusBarCell.get().setWidth(width);
    onResize(width, height);

  }

  public void layout() {
    layout(width, height);
  }

  public Window() {
  }

  public void init() {
    var that = this;
    this.table = new Table();
    this.titleBarCell = table.table(titleBar -> {
      this.titleTextBarCell = titleBar.table((titleTextBar) -> {
        titleTextBar
            .label(() -> getTitleConntext())
            .grow().get().setAlignment(Align.center);
      }).grow().left();
      this.titleTextBarCell.get().setBackground(StyleManager.style.titleTextBackground);
      this.titleTextBarCell.get().addListener(new InputListener() {
        @Override
        public void touchDragged(InputEvent event, float tx, float ty, int pointer) {
          if (draggable && !fullscreen) {
            positionParent(tx, ty);
          }
        }

        @Override
        public void touchUp(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
          moving = false;
        }

        @Override
        public boolean touchDown(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
          if (draggable && !fullscreen) {
            moving = true;
          }
          return true;
        }
      });
      titleBar.table(rightBar -> {
        rightBar
            .button(Core.atlas.drawable("oxygen-core-hide"), StyleManager.style.windowButtons,
                () -> {
                  hide();
                })
            .uniform().right().grow().size(StyleManager.ButtonSize);
        rightBar
            .button(Icon.cancel, StyleManager.style.windowButtons, (() -> {
              close();
            })).uniform().right().grow().size(StyleManager.ButtonSize);
        resizeButtonCell = rightBar.button(
            Icon.resize,
            StyleManager.style.defaulti,
            () -> {
            }).uniform().right().grow().size(StyleManager.ButtonSize);
        resizeButtonCell.get().addListener(new InputListener() {

          @Override
          public void touchDragged(InputEvent event, float tx, float ty, int pointer) {
            resizeTo(tx, ty);
          }

          @Override
          public void touchUp(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
            resizing = false;
          }

          @Override
          public boolean touchDown(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
            resizing = true;
            return true;
          }
        });
        resizeButtonCell.get().visible(() -> {
          return resizeable;
        });
      }).right().growY();
      table.addListener(new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
          Manager.activeWindow = that;
          table.toFront();
          return true;
        };
      });
    });
    titleBarCell.get().setBackground(StyleManager.style.titleBarBackground);
    titleBarCell.fillX();
    table.row();
    bodyContainer = new Table();
    drawBody(bodyContainer);
    bodyCell = table.table(
        StyleManager.style.bodyBackground, bodyt -> {
          bodyScroll = bodyt.add(
              new ScrollPane(bodyContainer)).uniformX().grow().get();
        }).uniformX().fillX();
    table.row();
    statusBarContainer = new Table();
    drawStatus(statusBarContainer);
    this.statusBarCell = table.table(statusBar -> {
      statusBar.add(statusBarContainer).uniformX().height(StyleManager.ButtonSize).grow();
    }).uniformX().fillX().height(StyleManager.ButtonSize);
    this.statusBarCell.get().setBackground(StyleManager.style.statusBarBackground);
    table.update(() -> {
      if (!table.fillParent && !fullscreen) {
        table.color.a = moving || resizing ? draggedAlpha : 1f;
        Vec2 pos = table.localToParentCoordinates(Tmp.v1.set(0, 0));
        setPosition(
            Mathf.clamp(pos.x, 0, table.parent.getWidth() - table.getPrefWidth() / 2),
            Mathf.clamp(pos.y, 0, table.parent.getHeight() - table.getPrefHeight() / 2));
      }
    });
    Manager.windowManager.registerWindow(this);

  }

  public void drawBody(Table table) {

  }

  public void drawStatus(Table table) {

  }

  public String getTitleConntext() {
    return (titleFunc == null
        ? (title.startsWith("@") ? Core.bundle.get(title.substring(1)) : title)
        : titleFunc.get(this));
  }

  public void toFront() {
    table.toFront();
  }

  public void hide() {
    if (visible) {
      visible = false;
      table.visible = false;
      Manager.windowManager.changeVisible(visible, this);
      onHide();
    }
  }

  public float getHeight() {
    return fullscreen ? Manager.heightY() - StyleManager.ButtonSize : height;
  }

  public float getWidth() {
    return fullscreen ? Manager.widthX() : width;
  }

  public void setPosition(float x, float y) {
    table.setPosition(x, y);
    this.x = x;
    this.y = y;
  }

  public void center() {
    center = true;
    table.setOrigin(Align.center);
    table.setPosition(Mathf.floor((table.parent.getWidth() - table.getPrefWidth())),
        Mathf.floor((table.parent.getHeight() - table.getPrefHeight())));
  }

  public boolean isCenter() {
    return center;
  }

  public void close() {
    hide();
    Manager.windowManager.removeWindow(this);
    table.parent.removeChild(table);
    table.remove();
    this.onClose();
    closed = true;
  }

  public void show() {
    if (!added) {
      Manager.addElement(table);
      added = true;
      if (startX != -1 && startY != -1) {
        Time.run(10, () -> {

          setPosition(startX, startY);
        });
      }
    }
    if (!visible) {
      visible = true;
      table.visible = true;
      Manager.windowManager.changeVisible(visible, this);
      onShow();
    }
  }

  public void positionParent(float x, float y) {
    if (table.parent == null)
      return;
    setPosition(
        Mathf.clamp(x + this.x - titleTextBarCell.get().getWidth() / 2, 0,
            table.parent.getWidth() - table.getWidth() / 2),
        Mathf.clamp(y + this.y - 24, 0, table.parent.getHeight() - table.getHeight()));
  }

  public void resizeTo(float tx, float ty) {
    if (table.parent == null)
      return;
    float dw = tx + table.getWidth() - resizeButtonCell.get().getWidth() / 2;
    float dy = ty + table.getHeight() - resizeButtonCell.get().getHeight();
    resize(Math.abs(dw), Math.abs(dy));
  }
}
