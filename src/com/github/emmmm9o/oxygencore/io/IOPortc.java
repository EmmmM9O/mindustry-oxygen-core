package com.github.emmmm9o.oxygencore.io;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;

import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;

public interface IOPortc {
  public void read(Reads reads);

  public void write(Writes writes);

  public boolean inputItem(Item item, Building source);

  public boolean inputLiquid(Liquid liquid, Building source);

  public boolean outputItem(Item item, Building source);

  public boolean outputLiquid(Liquid liquid, Building source);

  public String getName();

  public String getDisplayName();

  public void display(Table table);

  public void infoDisplay(Table table);

  public void configureDisplay(Table table);

  public void draw(Point2 side);

  public IOPortType type();

  public IOBuild build();

  public void updatePort(IOPort port);

  public void clearData();

  public void remove();

  public int index();

  public Point2 edge();
}
