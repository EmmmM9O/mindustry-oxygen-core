package com.github.emmmm9o.oxygencore.content;

import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock;
import mindustry.type.*;
import mindustry.content.*;
import mindustry.world.*;
import static mindustry.type.ItemStack.*;

public class OxygenBlocks {
  public static Block oxygenMessageBlock;

  public static void init() {
    oxygenMessageBlock = new OxygenMessageBlock("oxygen-message-block") {
      {
        requirements(Category.logic, with(Items.graphite, 5, Items.copper, 5));
      }
    };
  }
}
