package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.func.Cons;
import arc.func.Func;
import arc.math.geom.Vec2;
import arc.util.Align;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;

public class TipTable extends Table {
  public Func<TipTable, Vec2> positioner;

  public boolean added;

  public void update_tip() {

  }

  public void update_pos() {
    var pos = positioner.get(this);
    setPosition(pos.x, pos.y, Align.bottomLeft);
  }

  public TipTable(Func<TipTable, Vec2> positioner, Drawable background) {
    super(background);
    this.positioner = positioner;
    update(() -> {
      update_tip();
      update_pos();
    });
  }

  public TipTable() {
  }

  public TipTable(Func<TipTable, Vec2> positioner, Drawable background, Cons<Table> run) {
    super(background, run);
    this.positioner = positioner;
    update(() -> {
      update_tip();
      update_pos();
    });
    table(run).grow();
  }

  public void hide() {
    visible = false;
  }

  public void show() {
    if (!added) {
      added = true;
      Manager.group.addChild(this);
    }
    visible = true;
  }

  public void close() {
    hide();
    Manager.group.removeChild(this);
    added=false;
  }
}
