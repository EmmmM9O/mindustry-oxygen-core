package com.github.emmmm9o.oxygencore.ui.selectors;

import arc.scene.ui.layout.Table;

public interface Selectable {
  public void select();

  public void display(Table table);// display detail info

  public void displayIcon(Table table, Runnable onClick);// display icon for select

  public String write();

  public boolean read(String text);

  public boolean isSelected();
}
