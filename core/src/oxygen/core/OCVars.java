/* (C) 2024 */
package oxygen.core;

import mindustry.mod.Mods.*;
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

  public static void preinit() {
    loader = new OCLoadRenderer();
    UIUtil.setLoadRenderer(loader);
    events.putEvents();
  }

  public static void init() {
    bus.init();
  }

  public static void laterInit() {

  }
}
