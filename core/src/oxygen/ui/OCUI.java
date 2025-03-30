/* (C) 2025 */
package oxygen.ui;

import arc.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.ui.fragments.*;
import oxygen.ui.dialogs.*;
import oxygen.ui.fragments.*;

public class OCUI {
  public static MenuFragmentI menuFragment;
  public static OCDialog settingsDialog;

  public static void load() {
    Log.info("OC UI Initlize");
    OCStyles.load();
    menuFragment = new OCMenuFragment();
    settingsDialog = new OCSettingsDialog();
    Events.on(ResizeEvent.class, event -> {
      OCDialog dialog = OCDialog.sceneGetDialog();
      if (dialog != null)
        dialog.updateScrollFocus();
    });
  }

  public static void modifyUI(UI ui) {
    ui.menufrag = menuFragment;
  }
}
