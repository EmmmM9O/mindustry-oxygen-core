/* (C) 2024 */
package oxygen.loader;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import oxygen.utils.*;

/**
 * MLProcessor
 */
public class MLProcessor {
  public static class Key implements Comparable<Key> {
    public Class<?> clazz;
    public int priority;

    public Key() {}

    public Key(Class<?> clazz, int priority) {
      this.clazz = clazz;
      this.priority = priority;
    }

    @Override
    public int hashCode() {
      return clazz.hashCode();
    }

    @Override
    public int compareTo(Key other) {
      return this.priority - other.priority;
    }
  }

  public ObjectMap<Key, RuntimeAnnotationProcessor> annotationProcessors;
  public AMarkResolver resolver;

  public MLProcessor() {
    resolver = new AMarkResolver();
    annotationProcessors = new ObjectMap<>();
  }

  public void init() {
    resolve();
    loadML();
  }

  public void resolve() {
    for (LoadedMod mod : Vars.mods.orderedMods()) {
      resolver.resolve(mod);
    }
  }

  public void loadAnnotation(Key key) {
    try {
      RuntimeAnnotationProcessor processor = annotationProcessors.get(key);
      Log.info("load annotation @", key.clazz);
      if (processor == null)
        throw new RuntimeException("has no processor");
      Seq<Object> list = resolver.get(key.clazz, null);
      if (list != null) {
        for (Object obj : list) {
          processor.process(obj);
        }
      }
    } catch (Throwable error) {
      Log.err("process annotation @ error @", key.clazz.toString(), error.toString());
    }
  }

  public void loadML() {
    for (Key key : annotationProcessors.keys().toSeq().sort()) {
      loadAnnotation(key);
    }
  }
}
