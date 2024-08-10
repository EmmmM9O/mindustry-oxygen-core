package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.io.IOPortType;

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

  public static class PortSelectorable extends OxygenContentSeletorItem<IOPortType> {
    @Override
    public OxygenContentType getContentType() {
      return OxygenContentType.io_port;
    }

    public PortSelectorable(IOPortType type) {
      super(type);
    }

    public PortSelectorable() {
      super();
    }
  }

  public static SelectorType<ItemSelectorable, Func<Item, Boolean>> itemSelector;
  public static SelectorType<PortSelectorable, Func<IOPortType, Boolean>> portSelector;
  public static RadioSelectorType<PortSelectorable, Func<IOPortType, Boolean>> radioPortSelector;
  public static RadioSelectorType<ItemSelectorable, Func<Item, Boolean>> radioItemSelector;

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
    portSelector = new SelectorType<PortSelectorable, Func<IOPortType, Boolean>>("port-selector", filter -> {
      var res = new Seq<PortSelectorable>();
      for (var port : Manager.content.io_ports()) {
        if (filter.get(port)) {
          res.add(new PortSelectorable(port));
        }
      }
      return res;
    }, ((Func<IOPortType, Boolean>) item -> true).getClass());
    radioPortSelector = new RadioSelectorType<PortSelectorable, Func<IOPortType, Boolean>>("radio-port-selector",
        filter -> {
          var res = new Seq<PortSelectorable>();
          for (var port : Manager.content.io_ports()) {
            if (filter.get(port)) {
              res.add(new PortSelectorable(port));
            }
          }
          return res;
        }, ((Func<IOPortType, Boolean>) item -> true).getClass());
    radioItemSelector = new RadioSelectorType<ItemSelectorable, Func<Item, Boolean>>("radio-item-selector", filter -> {
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
