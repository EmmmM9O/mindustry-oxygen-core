package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.func.Cons;
import arc.func.Prov;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;

public class TipTable extends Table {
  public Prov<Vec2> positioner;

  public boolean added;

  public void update_tip() {

  }

  public void update_pos() {
    var pos = positioner.get();
    setPosition(pos.x, pos.y);
  }

  public TipTable(Prov<Vec2> positioner, Drawable background) {
    super(background);
    this.positioner = positioner;
    update(() -> {
      update_tip();
      update_pos();
    });
  }

  public TipTable() {
  }

  public TipTable(Prov<Vec2> positioner, Drawable background, Cons<Table> run) {
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
    Manager.group.removeChild(this);
    remove();
  }
}
