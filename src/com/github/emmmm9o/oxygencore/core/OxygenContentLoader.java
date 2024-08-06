package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.content.IOPorts;
import com.github.emmmm9o.oxygencore.ctype.OxygenContent;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenMappableContent;
import com.github.emmmm9o.oxygencore.io.IOPortType;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.mod.Mods.LoadedMod;

@SuppressWarnings("unchecked")
public class OxygenContentLoader {
  public static Seq<OxygenContentType> contentTypes = new Seq<>();
  public Seq<Seq<OxygenContent>> contentMap = new Seq<>();
  public Seq<ObjectMap<String, OxygenMappableContent>> contentNameMap = new Seq<>();

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
    if (contentNameMap.get(content.id).containsKey(content.name)) {
      throw new IllegalArgumentException(
          "Two content objects cannot have the same name! (issue: '" + content.name + "')");
    }
    contentNameMap.get(content.id).put(content.name, content);
  }

  public <T extends OxygenContent> Seq<T> getBy(OxygenContentType type) {
    return (Seq<T>) contentMap.get(type.id);
  }

  public Seq<IOPortType> io_ports() {
    return getBy(OxygenContentType.io_port);
  }

  public <T extends OxygenMappableContent> T getByName(OxygenContentType type, String name) {
    var map = contentNameMap.get(type.id);
    if (map == null)
      return null;
    return (T) map.get(name);
  }
}
