package com.github.emmmm9o.oxygencore.io.ports;

import com.github.emmmm9o.oxygencore.io.IOPort;
import com.github.emmmm9o.oxygencore.io.IOPortType;

import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Item;

public class SingleItemPortType extends IOPortType {
  public SingleItemPortType(String name) {
    super(name);
  }

  public class SingleItemPort extends IOPort {
    public Item filteredItem = Items.copper;

    @Override
    public boolean inputItem(Item item, Building source) {
      return item == filteredItem;
    }

    @Override
    public boolean outputItem(Item item, Building source) {
      return item == filteredItem;
    }

  }
}
