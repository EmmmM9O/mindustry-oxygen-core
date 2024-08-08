package com.github.emmmm9o.oxygencore.io.ports;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.io.IOPortType;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;

/**
 * ForbiddenPort
 */
public class ForbiddenPortType extends IOPortType{
  public ForbiddenPortType(String name) {
    super(name);
  }
  public class ForbiddenPort extends IOPort{
    public ForbiddenPort(IOBuild build, int index, IOPortType type) {
      super(build, index, type);
    }

    @Override
    public boolean inputItem(Item item, Building source) {
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
    public boolean outputLiquid(Liquid liquid, Building source) {
        return false;
    }
    
  }
}