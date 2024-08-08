
package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;
import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.func.Cons;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;

/**
 * OxygenContentSeletorItem
 */
public class OxygenContentSeletorItem<T extends OxygenInfoContent> implements Selectable {
  public T content;
  public boolean selected;
  public ImageButton iconButton;

  public OxygenContentType getContentType() {
    return null;
  }

  @Override
  public void display(Table table) {
    content.display(table);
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
        onClick).size(StyleManager.XButtonSize).uniform().get();
  }

  public void updateIcon() {
    if (iconButton != null) {
      iconButton.setStyle(selected ? StyleManager.style.selectedButton : StyleManager.style.windowButtons);
      iconButton.replaceImage(new Image(new TextureRegionDrawable(content.uiIcon)));
    }
  }

  public OxygenContentSeletorItem(T content, boolean selected) {
    this.content = content;
    this.selected = selected;
  }

  public boolean isSame(Selectable other) {
    return content == ((OxygenContentSeletorItem) other).content;
  }

  public OxygenContentSeletorItem(T content) {
    this(content, false);
  }

  public OxygenContentSeletorItem() {
    this(null, false);
  }

  @Override
  public boolean read(String text) {
    content = Manager.content.getByName(getContentType(), text);
    return content == null;
  }

  @Override
  public String write() {
    return content.name;
  }
}
