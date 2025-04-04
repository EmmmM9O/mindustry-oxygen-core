/* (C) 2024 */
package oxygen.core;

import arc.*;
import arc.files.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import oxygen.graphics.*;
import oxygen.loader.*;
import oxygen.ui.*;
import oxygen.utils.*;

/**
 * OCVars
 */
public class OCVars {
  @ML.Instance(OCMain.name)
  public static LoadedMod mod;

  @ML.Instance(OCMain.name)
  public static OCMain instance;

  @ML.Instance(OCMain.name)
  public static ModMeta meta;

  public static OPQEvent events = new OPQEvent();
  public static EventBus bus = new EventBus(events);
  public static OCLoadRenderer loader;
  public static OCRenderer renderer;

  public static void preinit() {
    loader = new OCLoadRenderer();
    UIUtil.setLoadRenderer(loader);
    events.putEvents();
    Core.app.addListener(renderer = new OCRenderer());
  }

  public static void init() {
    mod = Vars.mods.getMod(OCMain.name);
    bus.init();
    OCShaders.init();
  }

  public static void laterInit() {

  }

  public static Fi getTexture(String name) {
    return Vars.tree.get("textures/" + name);
  }
}
