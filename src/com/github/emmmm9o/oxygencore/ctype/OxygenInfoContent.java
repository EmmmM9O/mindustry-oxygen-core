package com.github.emmmm9o.oxygencore.ctype;

import arc.Core;
import arc.util.Nullable;
import mindustry.mod.Mods.LoadedMod;

public abstract class OxygenInfoContent extends OxygenMappableContent {
  public String localizedName;
  public @Nullable String description, details;

  public OxygenInfoContent(String name, LoadedMod mod) {
    super(name, mod);
    this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
    this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
    this.details = Core.bundle.getOrNull(getContentType() + "." + this.name + ".details");
  }

  public String displayDescription() {
    return mod == null ? description : description + "\n" + Core.bundle.format("mod.display", mod.meta.displayName());
  }

}
