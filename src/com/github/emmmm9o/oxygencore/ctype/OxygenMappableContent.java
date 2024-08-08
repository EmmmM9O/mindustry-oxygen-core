package com.github.emmmm9o.oxygencore.ctype;

import com.github.emmmm9o.oxygencore.core.Manager;

import mindustry.mod.Mods.LoadedMod;

public abstract class OxygenMappableContent extends OxygenContent {
  public final String name,orginName;

  public OxygenMappableContent(String name, LoadedMod mod) {
    super(mod);
    this.orginName=name;
    this.name = Manager.content.transformName(mod, name);
    Manager.content.handleMappableContent(this);
  }

  @Override
  public String toString() {
    return name;
  }
}
