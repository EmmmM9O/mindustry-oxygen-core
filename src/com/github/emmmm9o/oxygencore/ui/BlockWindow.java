package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.util.Util;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;

import mindustry.gen.Building;
import mindustry.gen.Icon;

import mindustry.world.Block;

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
      Util.drawContent(table, block());
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
