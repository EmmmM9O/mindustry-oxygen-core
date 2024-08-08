
package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;

import arc.func.Cons;
import arc.scene.ui.layout.Table;

/**
 * OxygenContentSeletorItem
 */
public class OxygenContentSeletorItem<T extends OxygenInfoContent> implements Selectable {
  public T content;
  public boolean selected;
  public Cons<Boolean> updateIcon;

  @Override
  public void display(Table table) {
    content.display(table);
  }

  @Override
  public void select() {
    this.selected = !this.selected;
    if (updateIcon != null) {
      updateIcon.get(selected);
    }
  }

  @Override
  public boolean isSelected() {
    return selected;
  }

  @Override
  public void displayIcon(Table table, Runnable onClick) {
    updateIcon = content.displaySelectIcon(table, onClick, selected);

  }

  public OxygenContentSeletorItem (T content, boolean selected) {
    this.content = content;
    this.selected = selected;
  }
public boolean isSame(Selectable other){
	return content==((OxygenContentSeletorItem)other).content;
}
  public OxygenContentSeletorItem(T content) {
    this(content, false);
  }
  public OxygenContentSeletorItem() {
    this(null, false);
  }
  @Override
  public boolean read(String text) {
    content = Manager.content.getByName(content.getContentType(), text);
    return content == null;
  }

  @Override
  public String write() {
    return content.name;
  }
}
