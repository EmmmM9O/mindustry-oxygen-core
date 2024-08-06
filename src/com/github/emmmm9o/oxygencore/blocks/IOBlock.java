package com.github.emmmm9o.oxygencore.blocks;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.io.IOPort;
import com.github.emmmm9o.oxygencore.io.IOPortType;
import com.github.emmmm9o.oxygencore.meta.OxygenStat;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Edges;

public class IOBlock extends BasicWindowBlock {
  public int portNumber;

  public IOBlock(String name, int size) {
    super(name);
    this.size = size;
    portNumber = size * 4;
    hasItems = true;
    hasLiquids = true;
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.add(OxygenStat.hasPorts, true);
  };

  public class IOBuild extends BasicWindowBuild {
    public Seq<IOPort> ports;

    @Override
    public void created() {
      ports = new Seq<>(portNumber);
      for (int i = 0; i < portNumber; i++)
        ports.add(IOPort.nonePort);
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
            if (super.acceptItem(source, item)) {
              return true;
            }
          }
        }
      }
      return false;
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
            if (super.acceptLiquid(source, liquid)) {
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
            if (super.canDump(to, item)) {
              return true;
            }
          }
        }
      }
      return false;
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
            if (super.canDumpLiquid(to, liquid)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    public void changePort(int index, IOPort port) {
      ports.remove(index).remove();
      port.clearData();
      ports.insert(index, port);
      for (var por : ports) {
        if (por != port) {
          por.updatePort(port);
          port.updatePort(por);
        }
      }
    }

    @Override
    public void drawConfigure() {
      for (var port : ports) {
        port.draw();
      }
    }

    @Override
    public void write(Writes write) {
      for (var port : ports) {
        write.str(port.getName());
        port.write(write);
      }
    }

    @Override
    public void read(Reads read) {
      ports = new Seq<>(portNumber);
      for (int i = 0; i < portNumber; i++) {
        var name = read.str();
        var type = (IOPortType) Manager.content.getByName(OxygenContentType.io_port, name);
        ports.add(type.readFrom(read, this, i));
      }
      updateAllPort();
    }

    public void updateAllPort() {
      for (var port : ports) {
        for (var port2 : ports) {
          if (port != port2)
            port.updatePort(port2);
        }
      }
    }
  }

}
