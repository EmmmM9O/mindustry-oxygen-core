package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.util.Util;

import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;

/**
 * MdtContentSelectorItem
 */
public class MdtContentSelectorItem<T extends UnlockableContent> implements Selectable {
  public T content;
  public boolean selected;
  public ImageButton iconButton;

  @Override
  public void display(Table table) {
    Util.drawContent(table, content);
  }

  public void updateIcon() {
    iconButton.setStyle(selected ? StyleManager.style.selectedButton : StyleManager.style.windowButtons);
  }

  @Override
  public void select() {
    this.selected = !this.selected;
    updateIcon();
  }

  @Override
  public boolean isSelected() {
    return selected;
  }

  @Override
  public void displayIcon(Table table, Runnable onClick) {
    iconButton = table.button(new TextureRegionDrawable(content.uiIcon),
        selected ? StyleManager.style.selectedButton : StyleManager.style.windowButtons,
        onClick).size(64).uniform().get();
  }

  public MdtContentSelectorItem(T content, boolean selected) {
    this.content = content;
    this.selected = selected;
  }

  public MdtContentSelectorItem(T content) {
    this(content, false);
  }

  @Override
  public boolean read(String text) {
    content = Vars.content.getByName(content.getContentType(), text);
    return content == null;
  }

  @Override
  public String write() {
    return content.name;
  }

}
