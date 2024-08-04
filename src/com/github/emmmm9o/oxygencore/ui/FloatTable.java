package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.gen.Icon;

public class FloatTable extends Table {
  public boolean added, isMoving;
  public static float draggedAlpha = 0.45f;
  public Cell<ImageButton> movingButtonCell;
  public Cell<Table> buttonsCell;
  public Table buttons;

  public void onTouchUp() {
  }

  public FloatTable() {
    super();
    setOrigin(Align.topLeft);
    buttonsCell=table(t->{
    movingButtonCell = t.button(Icon.move, StyleManager.style.windowButtons, () -> {
    }).uniform().fill().left().size(48);
    movingButtonCell.get().addListener(new InputListener() {
      @Override
      public void touchDragged(InputEvent event, float tx, float ty, int pointer) {
        positionParent(tx, ty);
      }

      @Override
      public void touchUp(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
        isMoving = false;
        onTouchUp();
      }

      @Override
      public boolean touchDown(InputEvent event, float tx, float ty, int pointer, KeyCode button) {
        isMoving = true;
        return true;
      }
    });
    });
    buttons=buttonsCell.get();
    update(() -> {
      color.a = isMoving ? draggedAlpha : 1f;
      Vec2 pos = localToParentCoordinates(Tmp.v1.set(0, 0));
      setPosition(
          Mathf.clamp(pos.x, getPrefWidth() / 2, parent.getWidth() - getPrefWidth() / 2),
          Mathf.clamp(pos.y, getPrefHeight() / 2, parent.getHeight() - getPrefHeight() / 2));
    });
  }

  public void show() {
    if (!added) {
      added = true;
      Manager.addElement(this);
    }
    if (!visible) {
      visible = true;
    }
  }

  public void positionParent(float x, float y) {
    if (parent == null)
      return;
//    Vec2 pos = movingButtonCell.get() .localToAscendantCoordinates(parent, Tmp.v1.set(x, y));
   // Vec2 pos2 = movingButtonCell.get().parentToLocalCoordinates(Tmp.v1.set(0,0));
    
    setPosition(
        Mathf.clamp(x+this.x-24, 0, parent.getWidth() - getPrefWidth() / 2),
        Mathf.clamp(y+this.y-24, 0, parent.getHeight() - getPrefHeight() / 2));
  }
}
