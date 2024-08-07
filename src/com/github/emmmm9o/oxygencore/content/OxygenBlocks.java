package com.github.emmmm9o.oxygencore.content;

import com.github.emmmm9o.oxygencore.blocks.IOBlock;
import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock;
import mindustry.type.*;
import mindustry.content.*;
import mindustry.world.*;
import static mindustry.type.ItemStack.*;

public class OxygenBlocks {
  public static Block oxygenMessageBlock, testOxygenIOBlock;

  public static void load() {
    oxygenMessageBlock = new OxygenMessageBlock("message-block") {
      {
        requirements(Category.logic, with(Items.graphite, 5, Items.copper, 5));
      }
    };
    testOxygenIOBlock = new IOBlock("test-io-block", 2) {
      {
        requirements(Category.distribution, with(Items.graphite, 5, Items.copper, 5));
        itemCapacity = 500;
      }
    };
  }
}
