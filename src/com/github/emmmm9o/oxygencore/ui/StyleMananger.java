package com.github.emmmm9o.oxygencore.ui;

import java.util.HashMap;
import java.util.Map;

import arc.Core;
import arc.scene.style.Drawable;
import arc.util.Log;
import arc.scene.ui.ImageButton;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

public class StyleMananger {
  public static class OxygenStyle {
    public ImageButton.ImageButtonStyle defaulti;
    public Drawable background;

    public OxygenStyle() {
    }
  }

  public static OxygenStyle style, defaultStyle = new OxygenStyle() {
    {
      this.defaulti = Styles.defaulti;
      this.background = Tex.button;
    }
  };
  public static Map<String, OxygenStyle> styles = new HashMap<String, OxygenStyle>();

  public static void init() {
    registerStyle("light", new OxygenStyle() {
      {
        this.defaulti = Styles.cleari;
        this.background = Styles.black6;
      }
    });
  }

  public static void load() {
    var currentStyle = Core.settings.get("oxygen-core-style", "light");
    Log.info("load style @", currentStyle);
    if (!styles.containsKey(currentStyle)) {
      style = defaultStyle;
    } else {
      style = styles.get(currentStyle);
    }
    Core.settings.put("oxygen-core-style", currentStyle);
  }

  public static void changeStyle(String name) {
    Core.settings.put("oxygen-core-style", name);
    load();
  }

  public static void registerStyle(String name, OxygenStyle sty) {
    styles.put(name, sty);
  }

}
