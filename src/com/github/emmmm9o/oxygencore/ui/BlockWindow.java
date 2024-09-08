package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.blocks.BasicWindowBlock.BasicWindowBuild;
import com.github.emmmm9o.oxygencore.util.Util;

import arc.scene.style.Drawable;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.world.Block;

public class BlockWindow extends Window {
  public Table bodyTable;
  public Building building;
  public int currentPage = 0;
  public Radio<Page> pageRadio;

  public static class Page {
    public int index;
    public Table table;
    public String name;
    public Drawable icon;
    public TextButton button;

    public Table drawButton(Table content) {
      return content.table(tab -> {
        if (icon != null)
          tab.image(icon).size(StyleManager.ButtonSize);
        else if (name != null)
          tab.add(name).size(StyleManager.ButtonSize).get().setAlignment(Align.center);
      }).size(StyleManager.ButtonSize).uniform().get();
    }

    public Page(String name, Table table) {
      this.name = name;
      this.table = table;
    }

    public Page(Drawable icon, Table table) {
      this.table = table;
      this.icon = icon;
    }
  }

  public Seq<Page> pages;

  public void choosePage(int page) {
    this.currentPage = page;
    bodyTable.clearChildren();
    bodyTable.add(pages.get(currentPage).table).grow();
  }

  public void addPage(Page page) {
    pages.add(page);
    page.index = pages.size - 1;
  }

  public void dinit() {
    addPage(new Page(Icon.edit, new Table(StyleManager.style.bodyBackground, control -> {
      drawControlTable(control);
    })));
    choosePage(0);
  }

  @Override
  public void drawBody(Table cont) {
    pages = new Seq<>();
    bodyTable = new Table();
    cont.add(bodyTable).grow();
    dinit();
    addPage(new Page(Icon.infoCircleSmall, new Table(StyleManager.style.bodyBackground, info -> {
      Util.drawContent(info, block());
    })));

  }

  @Override
  public void drawStatus(Table table) {
    pageRadio = new Radio<BlockWindow.Page>(pages, pages.get(currentPage), Tex.clear,
        (page, tab) -> page.drawButton(tab), page -> {
          choosePage(page.index);
        }, false);
    table.table(buttons -> {
      buttons.add(pageRadio).height(StyleManager.ButtonSize).grow();
    }).uniform().height(StyleManager.ButtonSize).grow();
  }

  public BlockWindow() {
  }

  public BlockWindow(Building building) {
    this.building = building;
  }

  public void drawControlTable(Table table) {
    if (building instanceof BasicWindowBuild bwb) {
      bwb.displayWindowExtra(table);
    }
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
