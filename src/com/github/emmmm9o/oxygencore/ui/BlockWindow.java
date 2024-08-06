package com.github.emmmm9o.oxygencore.ui;

import arc.Core;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatValue;
import mindustry.world.meta.Stats;

public class BlockWindow extends Window {
  public Table topButtons, buildingInfo, blockInfo;
  public Cell<Table> topButtonsCell, buildingInfoCell, blockInfoCell;
  public Building building;
  public boolean showBuildingInfo, showBlockInfo;
  public int blockInfoIndex, buildingInfoIndex;

  @Override
  public void drawBody(Table cont) {
    topButtonsCell = cont.table(buttons -> {
      buttons.button(Icon.menu, StyleManager.style.windowButtons, () -> {
        showBuildingInfo = !showBuildingInfo;

      }).size(48).left();
      buttons.button(Icon.infoCircle, StyleManager.style.windowButtons, () -> {
        showBlockInfo = !showBlockInfo;

      }).size(48).left();
    }).height(48).top();
    topButtons = topButtonsCell.get();
    cont.row();
    cont.table(tab -> {
      drawControlTable(tab);
    }).uniformX().grow();
    cont.row();
    buildingInfoCell = cont.table(tab -> {
      building.display(tab);
    }).uniformX().grow().visible(() -> showBuildingInfo);
    buildingInfo = buildingInfoCell.get();
    cont.row();
    blockInfoCell = cont.table(table -> {
      var block = block();
      block.checkStats();
      table.table(title1 -> {
        title1.image(block.uiIcon).size(Vars.iconXLarge).scaling(Scaling.fit);
        title1
            .add("[accent]" + block.localizedName + "\n[gray]" + block.name)
            .padLeft(5);
      });
      table.row();
      if (block.description != null) {
        var any = block.stats.toMap().size > 0;

        if (any) {
          table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
          table.row();
        }

        table.add("[lightgray]" + block.displayDescription()).wrap().fillX().padLeft(any ? 10 : 0).width(500f)
            .padTop(any ? 0 : 10).left();
        table.row();

        if (!block.stats.useCategories && any) {
          table.add("@category.general").fillX().color(Pal.accent);
          table.row();
        }
      }
      Stats stats = block.stats;

      for (StatCat cat : stats.toMap().keys()) {
        OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);

        if (map.size == 0)
          continue;

        if (stats.useCategories) {
          table.add("@category." + cat.name).color(Pal.accent).fillX();
          table.row();
        }

        for (Stat stat : map.keys()) {
          table.table(inset -> {
            inset.left();
            inset.add("[lightgray]" + stat.localized() + ":[] ").left().top();
            Seq<StatValue> arr = map.get(stat);
            for (StatValue value : arr) {
              value.display(inset);
              inset.add().size(10f);
            }

          }).fillX().padLeft(10);
          table.row();
        }
      }

      if (block.details != null) {
        table
            .add("[gray]" + (block.unlocked() || !block.hideDetails ? block.details
                : Iconc.lock + " " + Core.bundle.get("unlock.incampaign")))
            .pad(6).padTop(20).width(400f).wrap().fillX();
        table.row();
      }
      block.displayExtra(table);

    }).uniformX().grow().visible(() -> showBlockInfo);
    blockInfo = blockInfoCell.get();

  }

  public BlockWindow() {
  }

  public BlockWindow(Building building) {
    this.building = building;
  }

  public void drawControlTable(Table table) {

  }

  public Block block() {
    return building.block;
  }

  @Override
  public void onClose() {
    if (building instanceof WindowListener listener) {
      listener.onWindowClose();
    }
  }
}
