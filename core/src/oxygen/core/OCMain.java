/* (C) 2024 */
package oxygen.core;

import arc.*;
import arc.util.*;
import mindustry.core.UI;
import mindustry.game.*;
import mindustry.mod.*;
import oxygen.annotations.generator.*;
import oxygen.content.*;
import oxygen.ui.*;

/**
 * OCMain
 */
@ModMetaG(name = OCMain.name, minGameVersion = "146", author = OCMain.author,
    displayName = OCMain.displayName, version = OCMain.version, hidden = true)
public class OCMain extends Mod implements ModModifier {
  public static final String name = "oxygencore", author = "emmmm9o(novarc)", version = "1.0.0",
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
    OCVars.laterInit();
  }

  @Override
  public void modifyUI(UI ui) {
    OCUI.load();
    OCUI.modifyUI(ui);
  }

  @Override
  public void loadContent() {
    OUnitTypes.load();
  }
}
