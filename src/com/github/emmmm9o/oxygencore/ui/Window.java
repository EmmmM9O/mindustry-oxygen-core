package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.Core;
import arc.func.Func;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Tmp;
import arc.util.Log;

public class Window {
  public String title = "Empty";
  public Func<Window, String> titleFunc = null;
  public Table table, bodyContainer, statusBarContainer;
  public float x, y;
  // public float lastX = 0, lastY = 0;
  public float width = 100, height = 100;
  public boolean resizeable = true, draggable = true, moving;
  public boolean visible, fullscreen, center, added;
  public Element body, statusBar;
  public static float draggedAlpha = 0.45f;
  protected Cell<Table> titleBarCell, statusBarCell, statusBarContainerCell, bodyCell;
  protected Cell<ScrollPane> titleTextBarCell;
  protected Cell<ImageButton> maximizeButtonCell;

  public boolean active() {
    return Manager.activeWindow == this;
  }

  public void onClose() {

  }

  public void onHide() {

  }

  public void onShow() {

  }

  public void onFullScreen(boolean fullscreen) {

  }

  public void onMove(float tx, float ty) {

  }

  public void onResize(float wi, float hi) {

  }

  protected void bResize(float x, float y, float startX, float startY) {
    if (fullscreen)
      return;
    var dwidth = x - startX;
    var dheight = y - startY;
    dwidth += width;
    dheight += height;
    dwidth = Math.max(dwidth, 96f);
    dheight = Math.max(dheight, 64f);
  }

  public void resize(float dwidth, float dheight) {
    width = dwidth;
    height = dheight;
    layout();
  }

  public void layout(float width, float height) {
    table.setSize(width, height + 64f);
    titleBarCell.size(width, 32f);
    titleTextBarCell.size(width - 32f * (4 + (resizeable ? 1 : 0)), 32f);
    this.bodyCell.size(width, height);
    this.statusBarCell.size(width, 32);
    this.statusBarContainerCell.size(width - 32, 32);
    onResize(width, height);
    if (this.body != null)
      this.body.updateVisibility();
    if (this.statusBar != null)
      this.statusBar.updateVisibility();
  }

  public void layout() {
    layout(width, height);
  }

  public Window() {
    var that = this;
    this.table = new Table();
    this.titleBarCell = table.table(titleBar -> {
      this.titleTextBarCell = titleBar.pane((titleTextBar) -> {
        titleTextBar
            .label(() -> (titleFunc == null
                ? (active() ? "[cyan]" : "[gray]")
                    + (title.startsWith("@") ? Core.bundle.get(title.substring(1)) : title)
                : titleFunc.get(this)));
      });
      this.titleTextBarCell.get().addListener(new InputListener() {
        protected float lastX = 0, lastY = 0;

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
            lastX = tx;
            lastY = ty;
          }
          return true;
        }
      });
      var resizeButtonCell = titleBar.button(
          Core.atlas.drawable("oxygen-core-resize"),
          StyleMananger.style.defaultImageButtonStyle,
          () -> {
          }).size(32f, 32f);
      resizeButtonCell.get().addListener(new InputListener() {
        protected float lastX = 0, lastY = 0;

        @Override
        public void touchDragged(InputEvent event, float tx, float ty, int pointer) {
          bResize(tx, ty, lastX, lastY);
        }

        @Override
        public void touchUp(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
          bResize(tx, ty, lastX, lastY);
        }

        @Override
        public boolean touchDown(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
          lastX = tx;
          lastY = ty;
          return true;
        }
      });
      resizeButtonCell.get().visible(() -> {
        return resizeable;
      });
      resizeButtonCell.get().resizeImage(16f);
      var minimizeButtonCell = titleBar
          .button(Core.atlas.drawable("oxygen-core-hide"), StyleMananger.style.defaultImageButtonStyle,
              () -> {
                hide();
              })
          .size(32f, 32f);
      minimizeButtonCell.get().resizeImage(16f);
      maximizeButtonCell = titleBar
          .button(Core.atlas.drawable("oxygen-core-fullscreen"), StyleMananger.style.defaultImageButtonStyle,
              () -> {
                fullscreen();
              })
          .size(32f, 32f);
      maximizeButtonCell.get().resizeImage(16f);
      maximizeButtonCell.get().setDisabled((() -> !this.resizeable));
      var closeButton = titleBar
          .button(Core.atlas.drawable("oxygen-core-close"), StyleMananger.style.defaultImageButtonStyle, (() -> {
		  close();
          })).size(32, 32);
      closeButton.get().resizeImage(16);

    });
    table.addListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
        Manager.activeWindow = that;
        table.toFront();
        return true;
      };
    });
    titleBarCell.get().setBackground(StyleMananger.style.button);
    table.row();
    bodyContainer = new Table();
    bodyCell = table.table(body -> {
      body.add(bodyContainer);
    });
    bodyCell.get().setBackground(StyleMananger.style.button);
    table.row();
    statusBarContainer = new Table();
    this.statusBarCell = table.table(statusBar -> {
      that.statusBarContainerCell = statusBar.table(statB -> {
        statB.add(statusBarContainer);
      });
    });
    this.statusBarCell.get().setBackground(StyleMananger.style.button);
    table.update(() -> {
	    if(!table.fillParent&&!fullscreen){
      table.color.a = moving ? draggedAlpha : 1f;
      Vec2 pos = table.localToParentCoordinates (Tmp.v1.set(0, 0));
      setPosition(
          Mathf.clamp(pos.x, table.getPrefWidth() / 2, table.parent.getWidth() - table.getPrefWidth() / 2),
          Mathf.clamp(pos.y, table.getPrefHeight() / 2, table.parent.getHeight() - table.getPrefHeight() / 2));
	    }
    });
   // table.setBackground(StyleMananger.style.button);
  }

  public void setBody(Element body) {
    bodyContainer.clear();
    this.body = body;
    bodyContainer.add(body);
  }

  public void setStatusBar(Element statusBar) {
    statusBarContainer.clear();
    this.statusBar = statusBar;
    statusBarContainer.add(statusBar);
  }

  public void toFront() {
    table.toFront();
  }

  public void fullscreen() {
    if (fullscreen) {
      fullscreen = false;
      center = false;
      table.fillParent=false;
      layout();
      flush();
     // table.setOrigin(Align.bottomLeft);
      table.setPosition(x, y);
      maximizeButtonCell.get().replaceImage(new Image(Core.atlas.drawable("oxygen-core-fullscreen")));
    } else {
      fullscreen = true;
     // table.fillParent=true;
      layout(Manager.width()-200, Manager.height() - 400);
      table.setPosition((100),200);
      Log.info("table at x:@ y:@",table.x,table.y);
      maximizeButtonCell.get().replaceImage(new Image(Core.atlas.drawable("oxygen-core-consume")));
    }
    onFullScreen(fullscreen);
  }

  public void hide() {
    if (visible) {
      visible = false;
      table.visible = false;
      onHide();
    }
  }

  public void flush() {
    if (fullscreen) {
      layout(Manager.widthX(), Manager.heightY() - 64);
    } else {
      layout();
    }
  }

  public float getHeight() {
    return fullscreen ? Manager.heightY() - 64 : height;
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
    table.setPosition(Mathf.floor((table.parent.getWidth()-table.getPrefWidth()) ),
        Mathf.floor((table.parent.getHeight()-table.getPrefHeight())));
  }

  public boolean isCenter() {
    return center;
  }

  public void close() {
    hide();
    table.parent.removeChild(table);
    table.remove();
    this.onClose();
  }

  public void show() {
    if (!added) {
      Manager.addElement(table);
      added = true;
    }
    if (!visible) {
      visible = true;
      table.visible = true;
      onShow();
    }
  }

  public void positionParent(float x, float y) {
    if (table.parent == null)
      return;
    var tab=titleTextBarCell.get();
    Vec2 pos = tab.localToAscendantCoordinates(table.parent, Tmp.v1.set(x, y));
    pos.x-=tab.getWidth()/2;
    pos.y-=table.getHeight()-16f;
    setPosition(
        Mathf.clamp(pos.x, tab.getPrefWidth() / 2, table.parent.getWidth() - table.getPrefWidth() / 2),
        Mathf.clamp(pos.y, tab.getPrefHeight() / 2, table.parent.getHeight() - table.getPrefHeight() / 2));

  }
}
