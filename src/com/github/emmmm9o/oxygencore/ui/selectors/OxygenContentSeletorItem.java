
package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;

import arc.scene.ui.layout.Table;

/**
 * OxygenContentSeletorItem
 */
public class OxygenContentSeletorItem<T extends OxygenInfoContent> implements Selectable {
  public T content;
  public boolean selected;

  @Override
  public void display(Table table) {
    content.display(table);
  }

  @Override
  public void select() {
    this.selected = !this.selected;
  }

  @Override
  public void displayIcon(Table table, Runnable onClick) {
    content.displaySelectIcon(table, onClick, selected);
  }

  public OxygenContentSeletorItem(T content, boolean selected) {
    this.content = content;
    this.selected = selected;
  }

  public OxygenContentSeletorItem(T content) {
    this(content, false);
  }

  @Override
  public void read(String text) {
    content = Manager.content.getByName(content.getContentType(), text);
  }

  @Override
  public String write() {
    return content.name;
  }
}
