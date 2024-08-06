package com.github.emmmm9o.oxygencore.ctype;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.util.Nullable;
import mindustry.mod.Mods.LoadedMod;

public abstract class OxygenContent implements Comparable<OxygenContent> {
  public short id;
  public final @Nullable LoadedMod mod;

  public abstract OxygenContentType getContentType();

  public OxygenContent(LoadedMod mod) {
    this.mod = mod;
    Manager.content.handleContent(this);
  }

  /** @return whether this is content from the base game. */
  public boolean isVanilla() {
    return mod == null;
  }

  @Override
  public int compareTo(OxygenContent c) {
    return Integer.compare(id, c.id);
  }

  @Override
  public String toString() {
    return getContentType().name + "#" + id;
  }
}
