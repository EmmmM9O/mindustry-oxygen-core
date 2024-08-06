package com.github.emmmm9o.oxygencore.io;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;

import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import mindustry.mod.Mods.LoadedMod;
import mindustry.world.Edges;

public abstract class IOPortType extends OxygenInfoContent {

  public IOPortType(String name, LoadedMod mod) {
    super(name, mod);
  }

  public IOPortType(String name) {
    this(name, Manager.mod);
  }

  public void display(Table table) {
  }

  public String getDisplayName() {
    return localizedName;
  }

  public abstract IOPort create(IOBuild build, int index, Point2 d);

  public IOPort readFrom(Reads read, IOBuild build, int index) {
    var r = create(build, index, Edges.getEdges(build.block.size)[index]);
    r.read(read);
    return r;
  }
}
