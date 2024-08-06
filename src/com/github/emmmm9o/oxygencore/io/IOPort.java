package com.github.emmmm9o.oxygencore.io;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;

import arc.graphics.g2d.Draw;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Edges;

public abstract class IOPort implements IOPortc {
  public static IOPort nonePort = null;
  public IOBuild build;
  public IOPortType type;
  public int index;
  public Point2 edge;

  public IOPort() {
  }

  public IOPort(IOBuild build, int index, IOPortType type) {
    this.build = build;
    this.type = type;
    this.index = index;
    this.edge = Edges.getEdges(build.block.size)[index];
  }

  @Override
  public IOPortType type() {
    return type;
  }

  @Override
  public IOBuild build() {
    return build;
  }

  @Override
  public String getName() {
    return type.name;
  }

  @Override
  public Point2 edge() {
    return edge;
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public String getDisplayName() {
    return type.getDisplayName();
  }

  @Override
  public boolean outputLiquid(Liquid liquid, Building source) {
    return false;
  }

  @Override
  public boolean outputItem(Item item, Building source) {
    return false;
  }

  @Override
  public boolean inputLiquid(Liquid liquid, Building source) {
    return false;
  }

  @Override
  public boolean inputItem(Item item, Building source) {
    return false;
  }

  public float getRotation() {
    // 0 bottom 1 top 2 left 3 right
    switch ((index + 1) % 4) {
      case 1:
        return 180f;
      case 2:
        return 0f;
      case 3:
        return 90f;
      case 0:
        return 270f;
      default:
        return 0;
    }
  }

  @Override
  public void draw() {
    Draw.rect(type.fullIcon, (edge.x + build.tileX()) * 8, (edge.y + build.tileY()) * 8, 8, 8, getRotation());
  }

  @Override
  public void display(Table table) {

  }

  @Override
  public void infoDisplay(Table table) {

  }

  @Override
  public void configureDisplay(Table table) {

  }

  @Override
  public void updatePort(IOPort port) {

  }

  @Override
  public void remove() {

  }

  @Override
  public void clearData() {

  }

  @Override
  public void read(Reads reads) {

  }

  @Override
  public void write(Writes writes) {

  }
}
