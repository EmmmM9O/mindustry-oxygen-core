package com.github.emmmm9o.oxygencore.ui;

import java.util.HashMap;
import java.util.Map;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Log;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Dialog.DialogStyle;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class StyleManager {
  public static class OxygenStyle {
    public ImageButton.ImageButtonStyle defaulti, windowButtons,
        selectedButton, none, select;
    public Drawable titleTextBackground,
        statusBarBackground, bodyBackground, titleBarBackground, buttonSelect, buttonUnselect;
    public DialogStyle fullscreenDialogStyle;

    public OxygenStyle() {
    }
  }

  public static final float ButtonSize = 48f;
  public static final float XButtonSize = 64f;
  public static Drawable accent6;
  public static OxygenStyle style;
  public static Map<String, OxygenStyle> styles = new HashMap<String, OxygenStyle>();

  public static void init() {
    var whiteui = (TextureRegionDrawable) Tex.whiteui;
    var tmp = new Color(Pal.accent);
    tmp = tmp.a(0.6f);
    accent6 = whiteui.tint(tmp);
    registerStyle("light", new OxygenStyle() {
      {
        this.defaulti = Styles.cleari;
        this.windowButtons = Styles.cleari;
        this.bodyBackground = Styles.black3;
        this.titleBarBackground = Styles.black3;
        this.titleTextBackground = Styles.black5;
        this.statusBarBackground = Styles.black5;
        this.none = Styles.emptyi;
        this.select = Styles.selecti;
        this.buttonSelect = Tex.buttonSelect;
        this.buttonUnselect = Styles.none;
        this.fullscreenDialogStyle = new DialogStyle() {
          {
            this.background = Tex.windowEmpty;
            this.stageBackground = Styles.black3;
            this.titleFont = Fonts.def;
            this.titleFontColor = Pal.accent;
          }
        };
        this.selectedButton = new ImageButtonStyle() {
          {
            down = Styles.flatDown;
            up = Styles.accentDrawable;
            over = Styles.flatOver;
            imageUpColor = Color.white;
          }
        };
      }
    });
  }

  public static void load() {
    var currentStyle = Core.settings.get("oxygen-core-style", "light");
    Log.info("[oxygen-core-style] load style @", currentStyle);
    if (!styles.containsKey(currentStyle)) {
      Log.err("[oxygen-core-style] no style named @", currentStyle);
      changeStyle("light");
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
