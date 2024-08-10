package com.github.emmmm9o.oxygencore.io.ports;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.io.IOPortType;
import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.ui.selectors.SelectorType;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors;
import com.github.emmmm9o.oxygencore.ui.selectors.SelectorType.Selector;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors.ItemSelectorable;
import com.github.emmmm9o.oxygencore.core.Manager;

import arc.util.Tmp;
import arc.func.Func;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.Vars;
import mindustry.content.Items;

public class SingleItemPortType extends IOPortType {
  public SingleItemPortType(String name) {
    super(name);
    portType = (a, b) -> new SingleItemPort(a, b, this);
  }

  public class SingleItemPort extends IOPort {
    public Item filteredItem = Items.copper;
    public Image itemImage;
    public ImageButton button;
    public SelectorType<ItemSelectorable, Func<Item, Boolean>>.Selector selector;

    public SingleItemPort(IOBuild build, int index, IOPortType type) {
      super(build, index, type);
    }

    public void initSelector() {
      if (selector == null) {
        selector = Selectors.radioItemSelector.create_radio(tab -> {
          var dx = 0f;
          var dy = 0f;
          if (button != null) {
		  var k=button.localToAscendantCoordinates(Manager.group, Tmp.v1.set(0, 0));
            dx = k.x;
            dy = k.y-tab.getPrefHeight()-16;
          }
          return new Vec2(dx, dy);
        }, item -> {
          configItem(item == null ? null : item.content);
        }, item -> true);
        selector.visible = false;
      }
    }

    @Override
    public boolean inputItem(Item item, Building source) {
      return item.name == filteredItem.name;
    }

    @Override
    public boolean outputItem(Item item, Building source) {
      return item.name == filteredItem.name;
    }

    @Override
    public void infoDisplay(Table table) {
      table.table(tab -> {
        tab.add("Current Item").height(StyleManager.XButtonSize).grow();
        itemImage = tab.image(filteredItem != null ? new TextureRegionDrawable(filteredItem.uiIcon) : Icon.cancel)
            .size(StyleManager.XButtonSize).get();
      }).height(StyleManager.XButtonSize).grow().update(tab -> {
        if (!tab.visible && selector.visible) {
          selector.close();
        }
      });
    }

    public void configItem(Item item) {
      this.filteredItem = item;
      if (itemImage != null)
        itemImage.setDrawable(item == null ? Icon.cancel : new TextureRegionDrawable(item.fullIcon));
      if (button != null)
        button.replaceImage(new Image(item == null ? Icon.add : new TextureRegionDrawable(item.uiIcon)));

    }

    @Override
    public void configureDisplay(Table table) {
      button = table.button(
          filteredItem == null ? Icon.add : new TextureRegionDrawable(filteredItem.uiIcon),
          StyleManager.style.windowButtons, () -> {
            initSelector();
            if (selector.visible)
              selector.close();
            else
              selector.show();
          }).size(StyleManager.XButtonSize).get();
    }

    @Override
    public void write(Writes writes) {
      writes.str(filteredItem == null ? "null" : filteredItem.name);
    }

    @Override
    public void read(Reads reads) {
      var str = reads.str();
      if (str == null) {
        filteredItem = null;
      } else {
        filteredItem = Vars.content.item(str);
      }
    }
  }
}
