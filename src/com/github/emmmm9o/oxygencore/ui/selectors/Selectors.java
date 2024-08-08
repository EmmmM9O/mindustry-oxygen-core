package com.github.emmmm9o.oxygencore.ui.selectors;

import arc.func.Func;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.type.Item;

/**
 * Selectors
 */
public class Selectors {
  public static class ItemSelectorable extends MdtContentSelectorItem<Item> {
    @Override
    public ContentType getContentType() {
      return ContentType.item;
    }

    public ItemSelectorable(Item item) {
      super(item);
    }
    public ItemSelectorable() {
      super();
    }
  }

  public static SelectorType<ItemSelectorable, Func<Item, Boolean>> itemSelector;

  public static void init() {
    itemSelector = new SelectorType<ItemSelectorable, Func<Item, Boolean>>("item-selector", filter -> {
      var res = new Seq<ItemSelectorable>();
      for (var item : Vars.content.items()) {
        if (filter.get(item)) {
          res.add(new ItemSelectorable(item));
        }
      }
      return res;
    }, ((Func<Item, Boolean>) item -> true).getClass());
  }
}
