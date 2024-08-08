package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.util.Util;

import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ctype.ContentType;
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
    if (iconButton != null) {
      iconButton.setStyle(selected ? StyleManager.style.selectedButton : StyleManager.style.windowButtons);
      iconButton.replaceImage(new Image(new TextureRegionDrawable(content.uiIcon)));
    }
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

  public ContentType getContentType() {
    return null;
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

  public MdtContentSelectorItem() {
    this(null, false);
  }

  public static MdtContentSelectorItem create() {
    return new MdtContentSelectorItem();
  }

  @Override
  public boolean read(String text) {
    content = Vars.content.getByName(getContentType(), text);
    return content == null;
  }

  @Override
  public String write() {
    return content.name;
  }

  public boolean isSame(Selectable other) {
    return content == ((MdtContentSelectorItem) other).content;
  }
}
