package com.github.emmmm9o.oxygencore.content;

import com.github.emmmm9o.oxygencore.blocks.IOBlock;
import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock;
import com.github.emmmm9o.oxygencore.blocks.distribution.ItemAllocator;

import mindustry.type.*;
import mindustry.content.*;
import mindustry.world.*;
import static mindustry.type.ItemStack.*;

public class OxygenBlocks {
  public static Block messageBlock /* ,testOxygenIOBlock */
      , baseItemAllocator;

  public static void load() {
    messageBlock = new OxygenMessageBlock("message-block") {
      {
        requirements(Category.logic, with(Items.silicon, 5, Items.graphite, 10, Items.copper, 5));
      }
    };/*
       * testOxygenIOBlock = new IOBlock("test-io-block", 2) {
       * {
       * requirements(Category.distribution, with(Items.graphite, 5, Items.copper,
       * 5));
       * itemCapacity = 500;
       * }
       * };
       */
    baseItemAllocator = new ItemAllocator("base-item-allocator", 1) {
      {
        requirements(Category.distribution, with(Items.titanium, 5, Items.copper, 5, Items.graphite, 10));
        buildCostMultiplier = 4f;
      }
    };
  }
}
