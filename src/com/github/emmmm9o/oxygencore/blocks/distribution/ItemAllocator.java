package com.github.emmmm9o.oxygencore.blocks.distribution;

import com.github.emmmm9o.oxygencore.blocks.IOBlock;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;

/**
 * ItemAllocator
 */
public class ItemAllocator extends IOBlock {

  public float speed = 30f;

  public ItemAllocator(String name, int size) {
    super(name, size);
    this.size = size;
    solid = false;
    underBullets = true;
    update = true;
    hasItems = true;
    itemCapacity = 50;
    group = BlockGroup.transportation;
    unloadable = false;
    noUpdateDisabled = true;
  }

  public class ItemAllocatorBuild extends IOBuild {
    public float time;

    @Override
    public void handleItem(Building source, Item item) {
      items.add(item, 1);
      time = 0f;
    }

    @Override
    public void updateTile() {
      time += 1f / speed * delta();
      if (time >= 1f) {
        dump();
      }
    }
  }
}
