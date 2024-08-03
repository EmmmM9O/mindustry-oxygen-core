package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.ui.Window;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.WidgetGroup;

public class Manager {
  public static Window activeWindow;
  public static WidgetGroup group;

  public static float scale() {
    return (Core.settings.getInt("uiscale", 100) / 100 - 1) * 1.5f + 1;
  }

  public static float width() {
    return group.getWidth();
  }

  public static float height() {
    return group.getHeight();
  }

  public static float widthX() {
    return width() * scale();
  }

  public static float heightY() {
    return height() * scale();
  }

  public static void addElement(Element element) {
    group.addChild(element);
  }

  public static void init() {
    group = new WidgetGroup();
    group.fillParent = true;
    group.touchable = Touchable.childrenOnly;
    group.visible = true;
    Core.scene.add(group);
  }

}
