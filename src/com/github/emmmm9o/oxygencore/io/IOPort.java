package com.github.emmmm9o.oxygencore.io;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;

import arc.math.geom.Point2;

public abstract class IOPort implements IOPortc {
  public static IOPort nonePort = null;
  public final IOBuild build;
  public final IOPortType type;
  public final int index;
  public final Point2 edge;

  public IOPort(IOBuild build, IOPortType type, int index, Point2 edge) {
    this.build = build;
    this.type = type;
    this.index = index;
    this.edge = edge;
  }

  @Override
  public IOPortType type() {
    return type;
  }

  @Override
  public IOBuild build() {
    return build;
  }
}
