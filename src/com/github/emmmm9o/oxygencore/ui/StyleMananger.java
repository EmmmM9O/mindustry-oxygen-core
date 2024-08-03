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
    public ImageButton.ImageButtonStyle defaultImageButtonStyle;
    public Drawable button;

    public OxygenStyle() {
    }
  }

  public static OxygenStyle style, defaultStyle = new OxygenStyle() {
    {
      this.defaultImageButtonStyle = Styles.defaulti;
      this.button = Tex.button;
    }
  };
  public static Map<String, OxygenStyle> styles = new HashMap<String, OxygenStyle>();

  public static void init() {
    registerStyle("mindustry", new OxygenStyle() {
      {
        this.defaultImageButtonStyle = Styles.defaulti;
        this.button = Tex.button;
      }
    });
  }

  public static void load() {
    var currentStyle = Core.settings.get("oxygen-core-style", "mindustry");
    Log.info("load style @",currentStyle);
    if (!styles.containsKey(currentStyle)) {
      style = defaultStyle;
      Core.settings.put("oxygen-core-style", currentStyle);
    } else {
      style = styles.get(currentStyle);
    }

  }

  public static void changeStyle(String name) {
    Core.settings.put("oxygen-core-style", name);
    load();
  }

  public static void registerStyle(String name, OxygenStyle sty) {
    styles.put(name, sty);
  }

}
