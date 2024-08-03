package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.ui.Window;

import arc.Core;

public class Manager {
  public static Window activeWindow;

  public static float scale() {
    return (Core.settings.getInt("uiscale", 100) / 100 - 1) * 1.5f + 1;
  }

  public static float width() {
    return Core.scene.getWidth();
  }

  public static float height() {
    return Core.scene.getHeight();
  }

  public static float widthX() {
    return width() * scale();
  }

  public static float heightY() {
    return height() * scale();
  }

}
