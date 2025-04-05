/* (C) 2025 */
package oxygen.ctype;

import mindustry.mod.Mods.*;
import oxygen.core.*;

public abstract class OCContent implements Comparable<OCContent> {
  public String name;
  public short id;
  public LoadedMod mod;

  public OCContent(LoadedMod mod, String name) {
    OCVars.content.setCurrentMod(mod);
    this.mod = mod;
    this.name = OCVars.content.transformName(name);
    this.id = (short) OCVars.content.getBy(getContentType()).size;
    OCVars.content.handleContent(this);
  }

  public abstract String getContentType();

  public void init() {}

  public void postInit() {}

  public void load() {}

  public void loadIcon() {}

  @Override
  public int compareTo(OCContent c) {
    return Integer.compare(id, c.id);
  }

  @Override
  public String toString() {
    return "OC#" + getContentType() + "#" + id;
  }

}
