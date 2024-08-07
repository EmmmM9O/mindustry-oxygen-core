package com.github.emmmm9o.oxygencore.ui.selectors;

import arc.func.Func;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.type.Item;

/**
 * Selectors
 */
public class Selectors {
  public static SelectorType<MdtContentSelectorItem<Item>, Func<Item, Boolean>> itemSelector;

  public static void init() {
    itemSelector = new SelectorType<MdtContentSelectorItem<Item>, Func<Item, Boolean>>("item-selector", filter -> {
      var res = new Seq<MdtContentSelectorItem<Item>>();
      for (var item : Vars.content.items()) {
        if (filter.get(item)) {
          res.add(new MdtContentSelectorItem<Item>(item));
        }
      }
      return res;
    });
  }
}
