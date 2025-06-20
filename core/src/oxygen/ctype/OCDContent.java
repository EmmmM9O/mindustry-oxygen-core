/* (C) 2025 */
package oxygen.ctype;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.mod.Mods.*;
import oxygen.core.*;

public abstract class OCDContent extends OCContent {
  public String localizedName;
  public @Nullable String description, details;
  public TextureRegion uiIcon;
  public TextureRegion fullIcon;

  public OCDContent(String name) {
    this(OCVars.content.currentMod, name);
  }

  public OCDContent(LoadedMod mod, String name) {
    super(mod, name);
    this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
    this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
    this.details = Core.bundle.getOrNull(getContentType() + "." + this.name + ".details");
  }

  @Override
  public void loadIcon() {
    fullIcon = Core.atlas.find(getContentType() + "-" + name + "-full",
        Core.atlas.find(name + "-full", Core.atlas.find(name,
            Core.atlas.find(getContentType() + "-" + name, Core.atlas.find(name + "1")))));

    uiIcon = Core.atlas.find(getContentType() + "-" + name + "-ui", fullIcon);
  }

  public String displayDescription() {
    return mod == null ? description
        : description + "\n" + Core.bundle.format("mod.display", mod.meta.displayName);
  }
}
