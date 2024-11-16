/* (C) 2024 */
package oxygen.core;

import mindustry.mod.*;
import mindustry.mod.Mods.LoadedMod;
import oxygen.annotations.*;
import oxygen.core.loader.*;

/**
 * OCMain
 */
@ModMeta(name = OCMain.name, minGameVersion = "146", author = OCMain.author, displayName = OCMain.displayName, version = OCMain.version)
public class OCMain extends Mod {
  public static final String name = "oxygencore", author = "emmmm9o(novarc)", version = "1.0.0",
      displayName = "Oxygen Core";
  @ML.ModInstance(name)
  public LoadedMod mod;

  public OCMain() {

  };
}
