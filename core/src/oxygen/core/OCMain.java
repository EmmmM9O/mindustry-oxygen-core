/* (C) 2024 */
package oxygen.core;

import arc.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.mod.*;
import oxygen.annotations.*;

/**
 * OCMain
 */
@ModMeta(
        name = OCMain.name,
        minGameVersion = "146",
        author = OCMain.author,
        displayName = OCMain.displayName,
        version = OCMain.version,
        hidden = true)
public class OCMain extends Mod {
    public static final String name = "oxygencore",
            author = "emmmm9o(novarc)",
            version = "1.0.0",
            displayName = "Oxygen Core";

    public OCMain() {
        OCVars.preinit();
        Events.on(EventType.FileTreeInitEvent.class, event -> {
            OCVars.init();
        });
    }

    @Override
    public void init() {
        Log.info("oxygen init");
    }
}
