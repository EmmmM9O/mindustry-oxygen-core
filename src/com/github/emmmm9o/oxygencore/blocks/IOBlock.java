package com.github.emmmm9o.oxygencore.blocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.io.IOPortType;
import com.github.emmmm9o.oxygencore.io.IOPortType.IOPort;
import com.github.emmmm9o.oxygencore.meta.OxygenStat;
import com.github.emmmm9o.oxygencore.ui.IOPortBlockWindow;
import com.github.emmmm9o.oxygencore.util.Util;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Edges;
import mindustry.world.meta.Env;

public class IOBlock extends BasicWindowBlock {
  public transient int portNumber;
  public TextureRegion defaultPort;

  public IOBlock(String name, int size) {
    super(name);
    this.size = size;
    portNumber = size * 4;
    hasItems = true;
    hasLiquids = true;
    envEnabled = Env.any;
    drawDisabled = false;
    destructible = true;
    configurable = true;
    solid = true;
    config(byte[].class, (IOBuild build, byte[] data) -> {
      var reader = new Reads(new DataInputStream(new ByteArrayInputStream(data)));
      build.readT(reader);
      reader.close();
    });
  }

  @Override
  public void loadIcon() {
    super.loadIcon();
    defaultPort = Core.atlas.find("oxygen-core-IOBlock-default-port",
        "underline-over");
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.add(OxygenStat.hasPorts, true);
  };

  public class IOBuild extends BasicWindowBuild {
    public Seq<IOPort> ports;

    @Override
    public byte[] config() {
      var baos = new ByteArrayOutputStream();
      var writer = new Writes(new DataOutputStream(baos));
      writeT(writer);
      writer.close();
      return baos.toByteArray();
    }

    @Override
    public void created() {
      ports = new Seq<>(portNumber);
      for (int i = 0; i < portNumber; i++)
        ports.add(IOPort.nonePort);
    }

    public boolean acceptItemGen(Building source, Item item) {
      return (items.get(item) < getMaximumAccepted(item)) && team == source.team;
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
      var sides = Edges.getEdges(block.size);
      for (int i = 0; i < portNumber; i++) {
        var p = sides[i];
        if (Vars.world.build(p.x + tileX(), p.y + tileY()) == source) {
          var port = ports.get(i);
          if (port != null) {
            if (port.inputItem(item, source)) {
              return true;
            }
          } else {
            if (acceptItemGen(source, item)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    public boolean acceptLiquidGen(Building source, Liquid liquid) {
      return source.team == team;
    }

    @Override
    public void updateTableAlign(Table table) {
      Vec2 pos = Core.input.mouseScreen(x, y + size * Vars.tilesize / 2f + 1);
      table.setPosition(pos.x, pos.y, Align.bottom);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
      var sides = Edges.getEdges(block.size);
      for (int i = 0; i < portNumber; i++) {
        var p = sides[i];
        if (Vars.world.build(p.x + tileX(), p.y + tileY()) == source) {
          var port = ports.get(i);
          if (port != null) {
            if (port.inputLiquid(liquid, source)) {
              return true;
            }
          } else {
            if (acceptLiquidGen(source, liquid)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    @Override
    public boolean canDump(Building to, Item item) {
      var sides = Edges.getEdges(block.size);
      for (int i = 0; i < portNumber; i++) {
        var p = sides[i];
        if (Vars.world.build(p.x + tileX(), p.y + tileY()) == to) {
          var port = ports.get(i);
          if (port != null) {
            if (port.outputItem(item, to)) {
              return true;
            }
          } else {
            if (canDumpGen(to, item)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    public boolean canDumpGen(Building to, Item item) {
      return super.canDump(to, item);
    }

    @Override
    public boolean canDumpLiquid(Building to, Liquid liquid) {
      var sides = Edges.getEdges(block.size);
      for (int i = 0; i < portNumber; i++) {
        var p = sides[i];
        if (Vars.world.build(p.x + tileX(), p.y + tileY()) == to) {
          var port = ports.get(i);
          if (port != null) {
            if (port.outputLiquid(liquid, to)) {
              return true;
            }
          } else {
            if (canDumpLiquidGen(to, liquid)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    public boolean canDumpLiquidGen(Building to, Liquid liquid) {
      return super.canDumpLiquid(to, liquid);
    }

    public void clearS() {
      ports.clear();
      for (int i = 0; i < portNumber; i++)
        ports.add(IOPort.nonePort);
    }

    public void changePort(int index, IOPort port) {
      var t = ports.remove(index);
      if (t != null)
        t.remove();
      if (port != null)
        port.clearData();
      ports.insert(index, port);
      if (port != null) {
        for (var por : ports) {
          if (por != port && por != null && port != null) {
            por.updatePort(port);
            port.updatePort(por);
          }
        }
      }
    }

    @Override
    public void buildConfiguration(Table table) {
      table.button(Icon.settings, Styles.cleari, () -> {
        showWindow();
      });
    }

    @Override
    public void drawConfigure() {
      super.drawConfigure();
      int index = 0;
      for (var port : ports) {
        if (port != null) {
          port.draw();
        } else {
          var po = Edges.getEdges(size)[index];
          drawDefaultPort(po, index);
        }
        index++;
      }
    }

    public void drawDefaultPort(Point2 edge, int index) {
      Draw.rect(defaultPort, (edge.x + tileX()) * 8, (edge.y + tileY()) * 8, 8, 8, Util.getRotation(index, size));
    }

    public void writeT(Writes write) {
      for (int i = 0; i < portNumber; i++) {
        var port = ports.get(i);
        if (port != null) {
          write.bool(false);
          write.str(port.getName());
          port.write(write);
        } else {
          write.bool(true);
        }
      }
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      var length = read.i();
      var data = read.b(length);
      var reader = new Reads(new DataInputStream(new ByteArrayInputStream(data)));
      readT(reader);
      reader.close();
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      var data = config();
      write.i(data.length);
      write.b(data);
    }

    public void readT(Reads read) {
      ports = new Seq<>(portNumber);
      for (int i = 0; i < portNumber; i++) {
        if (!read.bool()) {
          var name = read.str();
          var type = (IOPortType) Manager.content.getByName(OxygenContentType.io_port, name);
          if (type == null) {
            ports.add((IOPort) null);
          } else
            ports.add(type.readFrom(read, this, i));
        } else {
          ports.add((IOPort) null);
        }
      }
      updateAllPort();
    }

    public void updateAllPort() {
      for (var port : ports) {
        for (var port2 : ports) {
          if (port != port2 && port != null && port2 != null)
            port.updatePort(port2);
        }
      }
    }

    @Override
    public void initWindow() {
      window = new IOPortBlockWindow(this);
      window.init();
      setWindowStartPos();
    }

    @Override
    public void drawWindow() {
      window.resize(400f, 400f);
      window.title = getDisplayName();
    }

  }

}
