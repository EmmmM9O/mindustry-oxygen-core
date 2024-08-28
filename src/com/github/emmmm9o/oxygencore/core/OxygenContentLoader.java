package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.content.IOPorts;
import com.github.emmmm9o.oxygencore.ctype.OxygenContent;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenMappableContent;
import com.github.emmmm9o.oxygencore.io.IOPortType;
import com.github.emmmm9o.oxygencore.universe.OPlanet;
import com.github.emmmm9o.oxygencore.universe.OPlanets;
import com.github.emmmm9o.oxygencore.util.OxygenEventType;

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.mod.Mods.LoadedMod;

@SuppressWarnings("unchecked")
public class OxygenContentLoader {
  public static Seq<OxygenContentType> contentTypes = new Seq<>();
  public Seq<Seq<OxygenContent>> contentMap = new Seq<>();
  public Seq<ObjectMap<String, OxygenMappableContent>> contentNameMap = new Seq<>();
  private ObjectSet<Cons<OxygenContent>> initialization = new ObjectSet<>();

  private void initialize(Cons<OxygenContent> callable) {
    if (initialization.contains(callable))
      return;

    for (OxygenContentType type : contentTypes) {
      for (OxygenContent content : contentMap.get(type.id)) {
        try {
          callable.get(content);
        } catch (Throwable e) {
          if (content.mod != null) {
            Log.err("OxygenContent Load Mod[@] Error@", content.mod.name, e);
          } else {
            throw new RuntimeException(e);
          }
        }
      }
    }

    initialization.add(callable);
  }

  public void log() {
    Log.info("Oxygen-Core Content Loader :");
    Log.info("--- CONTENT INFO ---");
    for (int k = 0; k < contentMap.size; k++) {
      Log.info("[@]: loaded @", contentTypes.get(k).name, contentMap.get(k).size);
    }
    Log.info("Total content loaded: @", contentTypes.mapInt(c -> contentMap.get(c.id).size).sum());
    Log.info("-------------------");

  }

  public void init() {
    initialize(OxygenContent::init);
    Events.fire(new OxygenEventType.OxygenContentInitEvent());
  }

  public void load() {
    initialize(OxygenContent::loadIcon);
    initialize(OxygenContent::load);
  }

  public OxygenContentLoader() {

  }

  public void registerContent(OxygenContentType type) {
    contentTypes.add(type);
    contentMap.add(new Seq<OxygenContent>());
    type.id = contentMap.size - 1;
    contentNameMap.add(new ObjectMap<String, OxygenMappableContent>());
  }

  public void createBaseContent() {
    registerContent(OxygenContentType.io_port);
    IOPorts.load();
    registerContent(OxygenContentType.oplanet);
    OPlanets.load();
  }

  public String transformName(LoadedMod mod, String name) {
    return mod == null ? name : mod.name + "-" + name;
  }

  public void handleContent(OxygenContent content) {
    var list = contentMap.get(content.getContentType().id);
    content.id = (short) list.size;
    list.add(content);
  }

  public void handleMappableContent(OxygenMappableContent content) {
    if (contentNameMap.get(content.getContentType().id).containsKey(content.name)) {
      throw new IllegalArgumentException(
          "Two content objects cannot have the same name! (issue: '" + content.name + "')");
    }
    contentNameMap.get(content.getContentType().id).put(content.name, content);
  }

  public <T extends OxygenContent> Seq<T> getBy(OxygenContentType type) {
    return (Seq<T>) contentMap.get(type.id);
  }

  public Seq<IOPortType> io_ports() {
    return getBy(OxygenContentType.io_port);
  }

  public Seq<OPlanet> oplanets() {
    return getBy(OxygenContentType.oplanet);
  }

  public <T extends OxygenMappableContent> T getByName(OxygenContentType type, String name) {
    var map = contentNameMap.get(type.id);
    if (map == null)
      return null;
    return (T) map.get(name);
  }
}
