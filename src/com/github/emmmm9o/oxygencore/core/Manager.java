package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.graphics.ORenderers;
import com.github.emmmm9o.oxygencore.graphics.OShaders;
import com.github.emmmm9o.oxygencore.ui.BlockWindow;
import com.github.emmmm9o.oxygencore.ui.Window;
import com.github.emmmm9o.oxygencore.ui.WindowManager;
import com.github.emmmm9o.oxygencore.ui.dialogs.OxygenPlanetDialog;
import com.github.emmmm9o.oxygencore.ui.selectors.Selectors;
import com.github.emmmm9o.oxygencore.universe.OUniverse;
import com.github.emmmm9o.oxygencore.universe.UniverseRenderer;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.WidgetGroup;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.FileTreeInitEvent;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;

public class Manager {
  public static Window activeWindow;
  public static WidgetGroup group;
  public static WindowManager windowManager;
  public static OxygenContentLoader content;
  public static LoadedMod mod;
  public static OxygenPlanetDialog planet;
  public static OUniverse universe;
  public static ORenderers renderers;

  public static LoadedMod getLoadedMod(Class<? extends Mod> clazz) {
    return Vars.mods.getMod(clazz);
  }

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

  public static void saveManagerPosition() {
    Core.settings.put("oxygen-core-manager-x", Integer.toString((int) windowManager.x));
    Core.settings.put("oxygen-core-manager-y", Integer.toString((int) windowManager.y));
  }

  public static void loadManagerPosition() {
    windowManager.setPosition(
        Integer.parseInt(Core.settings.getString("oxygen-core-manager-x",
            Integer.toString((int) (width() / 2)))),
        Integer.parseInt(Core.settings.getString("oxygen-core-manager-y",
            Integer.toString((int) (height() / 2)))));
  }

  public static void init() {
    content = new OxygenContentLoader();

  }

  public static void initContent() {
    mod = getLoadedMod(CoreMod.class);
    if (!Vars.headless) {
      OShaders.init();
    }
    // it needs mod to call baseContent
    content.createBaseContent();

  }

  public static void initUI() {
    renderers = new ORenderers();
    renderers.init();
    universe = new OUniverse();
    universe.updateGlobal();
    group = new WidgetGroup();
    group.fillParent = true;
    group.touchable = Touchable.childrenOnly;
    group.visible = true;
    Core.scene.add(group);
    Selectors.init();
    planet = new OxygenPlanetDialog();
    windowManager = new WindowManager();
    addElement(windowManager);
    Time.run(10, () -> {
      loadManagerPosition();
    });
    Events.on(EventType.GameOverEvent.class, e -> {
      for (var w : windowManager.windows.keySet()) {
        if (w instanceof BlockWindow) {
          w.close();
        }
      }
    });
  }
}
