package oxygen.ui;

import mindustry.*;
import mindustry.graphics.*;

public class UIUtil {
  public static boolean isClient() {
    if (Vars.platform instanceof ClientLauncher)
      return true;
    else
      return false;
    //or return Vars.launcher != null;
  }

  public static ClientLauncher getClient() {
    if (Vars.platform instanceof ClientLauncher client)
      return client;
    return null;
    // Or return Vars.launcher;
  }

  public static void setLoadRenderer(LoadRendererI loadRenderer) {
    var client = getClient();
    if (client == null)
      return;
    var origin = client.loader;
    if (origin != null)
      origin.dispose();
    client.loader = loadRenderer;
  }
}
