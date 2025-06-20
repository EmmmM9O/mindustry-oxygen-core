/* (C) 2025 */
package oxygen.ctype;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.mod.Mods.*;

@SuppressWarnings("unchecked")
public class OCContentLoader {
  ObjectMap<String, ObjectMap<String, OCContent>> contentNameMap = new ObjectMap<>();
  ObjectMap<String, OCContent> nameMap = new ObjectMap<>();
  ObjectMap<String, Seq<OCContent>> contentMap = new ObjectMap<>();
  @Nullable
  LoadedMod currentMod;
  @Nullable
  OCContent lastAdded;
  ObjectSet<Cons<OCContent>> initialization = new ObjectSet<>();
  ObjectMap<LoadedMod, Runnable> loaders;

  public void setLoader(LoadedMod mod, Runnable loader) {
    loaders.put(mod, loader);
  }

  public void loadMods() {
    for (var e : loaders.entries()) {
      currentMod = e.key;
      e.value.run();
    }
  }

  public OCContentLoader() {
    Core.assets.loadRun("occontentinit", OCContentLoader.class, () -> this.init(),
        () -> this.load());
  }

  public void init() {
    initialize(OCContent::init);
    initialize(OCContent::postInit);
  }

  public void load() {
    initialize(OCContent::loadIcon);
    initialize(OCContent::load);
  }

  private void initialize(Cons<OCContent> callable) {
    if (initialization.contains(callable))
      return;
    for (Seq<OCContent> seq : contentMap.values()) {
      for (OCContent content : seq) {
        try {
          callable.get(content);
        } catch (Throwable e) {
          if (content.mod != null) {
            Log.err(e);
          } else {
            throw new RuntimeException(e);
          }
        }
      }
    }
    initialization.add(callable);
  }

  public @Nullable OCContent getLastAdded() {
    return lastAdded;
  }

  public void handleContent(OCContent content) {
    this.lastAdded = content;
    contentMap.get(content.getContentType(), Seq::new).add(content);
    ObjectMap<String, OCContent> map = contentNameMap.get(content.getContentType(), ObjectMap::new);
    if (map.containsKey(content.name)) {
      var list = contentMap.get(content.getContentType());
      if (list.size > 0 && list.peek() == content) {
        list.pop();
      }
      throw new IllegalArgumentException(
          "Two content objects cannot have the same name! (issue: '" + content.name + "')");
    }
    map.put(content.name, content);
    nameMap.put(content.name, content);
  }

  public void setCurrentMod(@Nullable LoadedMod mod) {
    this.currentMod = mod;
  }

  public String transformName(String name) {
    return currentMod == null ? name : currentMod.name + "-" + name;
  }

  public <T extends OCContent> Seq<T> getBy(String type) {
    return (Seq<T>) contentMap.get(type);
  }

  public @Nullable OCContent byName(String name) {
    return nameMap.get(name);
  }

  public ObjectMap<String, Seq<OCContent>> getContentMap() {
    return contentMap;
  }

  public void each(Cons<OCContent> cons) {
    for (Seq<OCContent> seq : contentMap.values()) {
      seq.each(cons);
    }
  }

  public <T extends OCContent> T getByName(String type, String name) {
    var map = contentNameMap.get(type);

    if (map == null)
      return null;
    return (T) map.get(name);
  }

  public <T extends OCContent> T getByID(String type, int id) {
    Seq<OCContent> list = contentMap.get(type);
    if (id >= list.size || id < 0) {
      return null;
    }
    return (T) list.get(id);
  }

}
