package com.github.emmmm9o.oxygencore.io;

import java.lang.reflect.Constructor;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;
import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.util.Util;

import arc.func.Func2;
import arc.graphics.g2d.Draw;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Structs;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Edges;

public class IOPortType extends OxygenInfoContent {
  public Func2<IOBuild, Integer, IOPort> portType;

  public IOPortType(String name, LoadedMod mod) {
    super(name, mod);
    initPort();
  }

  /* Do not use this unless you are work for the oxygen core */
  public IOPortType(String name) {
    this(name, Manager.mod);
  }

  @Override
  public OxygenContentType getContentType() {
    return OxygenContentType.io_port;
  }

  public String getDisplayName() {
    return localizedName;
  }

  public IOPort create(IOBuild build, int index) {
    return portType.get(build, index);
  }

  public IOPort readFrom(Reads read, IOBuild build, int index) {
    var r = create(build, index);
    r.read(read);
    return r;
  }

  public void initPort() {
    try {
      Class<?> current = getClass();

      while (portType == null && IOPortType.class.isAssignableFrom(current)) {
        Class<?> type = Structs.find(
            current.getDeclaredClasses(), t -> IOPort.class.isAssignableFrom(t) && !t.isInterface());
        if (type != null) {
          Constructor<? extends IOPort> cons = (Constructor<? extends IOPort>) type
              .getDeclaredConstructor(IOBuild.class, int.class, IOPortType.class);
          portType = (a, b) -> {
            try {
              return cons.newInstance(a, b, this);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };
        }
        current = current.getSuperclass();
      }

    } catch (Throwable ignored) {
      // throw new RuntimeException("Error " + ignored.toString());
    }
    if (portType == null) {
      portType = (a, b) -> new IOPort(a, b, this);
      // only for debug
      // throw new RuntimeException("Error class " + getClass().toString());
    }
  }

  public static class IOPort implements IOPortc {
    public static IOPort nonePort = null;

    public transient IOBuild build;
    public transient IOPortType type;
    public int index;
    public Point2 edge;

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

    @Override
    public void draw() {
      Draw.rect(type.fullIcon, (edge.x + build.tileX()) * 8, (edge.y + build.tileY()) * 8, 8, 8,
          Util.getRotation(index, build.block().size));
    }

    @Override
    public void display(Table table) {
      table.image(type.uiIcon).size(StyleManager.XButtonSize);
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
}
