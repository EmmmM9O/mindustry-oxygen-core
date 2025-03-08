/* (C) 2025 */
package oxygen.ui;

import arc.util.*;
import mindustry.core.*;
import mindustry.ui.fragments.*;
import oxygen.ui.fragments.*;

/**
 * OCUI
 */
public class OCUI {
  public static MenuFragmentI menuFragment;

  public static void load() {
    Log.info("OC UI Initlize");
    OCStyles.load();
    menuFragment = new OCMenuFragment();
  }

  public static void modifyUI(UI ui) {
    ui.menufrag = menuFragment;
  }
}
