/* (C) 2024 */
package oxygen.utils;

import arc.files.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.mod.Mods.*;

/**
 * AMarkReslover
 */
public class AMarkResolver {
    public ObjectMap<Class<?>, Seq<Object>> maps;

    private Class<?>[] asClassArray(Jval value) throws Throwable {
        Seq<Class<?>> list = Seq.with();
        for (Jval val : value.asArray()) {
            list.add(Class.forName(val.asString()));
        }
        return list.items;
    }

    private Object parserElememt(Jval value) throws Throwable {
        String type = value.get("type").asString();
        if (type.equals("CLASS") || type.equals("ENUM") || type.equals("INTERFACE") || type.equals("ANNOTATION_TYPE")) {
            return Class.forName(value.get("classpath").asString());
        }
        if (type.equals("FIELD") || type.equals("ENUM_CONSTANT")) {
            return Class.forName(value.get("classpath").asString())
                    .getDeclaredField(value.get("name").asString());
        }
        if (type.equals("METHOD")) {
            return Class.forName(value.get("classpath").asString())
                    .getDeclaredMethod(value.get("name").asString(), asClassArray(value.get("params")));
        }
        if (type.equals("CONSTRUCTOR")) {
            return Class.forName(value.get("classpath").asString())
                    .getDeclaredConstructor(asClassArray(value.get("params")));
        }
        throw new RuntimeException("unsupport type " + type);
    }

    public AMarkResolver resolve(String context) {
        try {
            Jval val = Jval.read(context);
            JsonMap json = val.asObject();
            for (Entry<String, Jval> pair : json) {
                try {
                    Class<?> clazz = Class.forName(pair.key);
                    Seq<Object> list = Seq.with();
                    for (Jval value : pair.value.asArray()) {
                        list.add(parserElememt(value));
                    }
                    if (maps.containsKey(clazz)) {
                        maps.get(clazz).addAll(list);
                    } else maps.put(clazz, list);
                } catch (Throwable err) {
                    Log.err("AMarkResolver err load class @ @", pair.key, err.toString());
                }
            }
        } catch (Throwable error) {
            Log.err("AMarkResolver load error @", error.toString());
        }
        return this;
    }

    public AMarkResolver resolve(LoadedMod mod) {
        Fi file = mod.root.child("marks.json");
        if (file.exists() && !file.isDirectory()) resolve(file);
        return this;
    }

    public AMarkResolver resolve(Fi file) {
        resolve(file.readString());
        return this;
    }

    public AMarkResolver() {
        this.maps = new ObjectMap<>();
    }

    public AMarkResolver(String context) {
        this();
        resolve(context);
    }

    public AMarkResolver(Fi file) {
        this();
        resolve(file);
    }

    public Seq<Object> get(Class<?> clazz) {
        return maps.get(clazz);
    }

    public Seq<Object> get(Class<?> clazz, Seq<Object> def) {
        return maps.get(clazz, def);
    }

    public boolean has(Class<?> clazz) {
        return maps.containsKey(clazz);
    }

    @Override
    public String toString() {
        return "AMarkResolver{" + maps.toString() + "}";
    }
}
