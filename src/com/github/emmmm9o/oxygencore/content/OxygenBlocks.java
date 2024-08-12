package com.github.emmmm9o.oxygencore.content;

import com.github.emmmm9o.oxygencore.blocks.IOBlock;
import com.github.emmmm9o.oxygencore.blocks.MulticraftBlock;
import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock;
import com.github.emmmm9o.oxygencore.blocks.distribution.ItemAllocator;
import com.github.emmmm9o.oxygencore.util.Formula;

import arc.graphics.Color;
import mindustry.type.*;
import mindustry.content.*;
import mindustry.gen.Sounds;
import mindustry.world.*;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawFlame;
import mindustry.world.draw.DrawMulti;

import static mindustry.type.ItemStack.*;

public class OxygenBlocks {
  public static Block messageBlock /* ,testOxygenIOBlock */
      , baseItemAllocator, testMulticraftBlock;

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
    testMulticraftBlock = new MulticraftBlock("test-multicraft-block", 1) {
      {
	      itemCapacity=100;
        drawer = new DrawMulti(new DrawDefault(), new DrawFlame(Color.valueOf("ffef99")));
        requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 25));

        ambientSound = Sounds.smelter;
        ambientSoundVolume = 0.07f;
        Formula formula = new Formula(with(Items.copper, 1, Items.lead, 1), with(Items.coal, 1), 0.5f, 20f),
            formula2 = new Formula(with(Items.copper, 1), with(Items.lead, 1), 1f, 120f);
        consumeFormula(formula, formula2);
      }
    };
  }
}
