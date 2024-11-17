/* (C) 2024 */
package oxygen.core;

import arc.util.*;
import mindustry.mod.Mods.*;
import oxygen.loader.*;
import oxygen.utils.*;

/**
 * OCVars
 */
public class OCVars {
    @ML.Instance(OCMain.name)
    public static LoadedMod mod;

    public static EventBus bus = new EventBus();

    public static void init() {
        bus.init();
        Log.info("test @", mod.name);
    }
}
