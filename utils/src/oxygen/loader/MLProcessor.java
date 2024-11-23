/* (C) 2024 */
package oxygen.loader;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import java.lang.reflect.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import oxygen.utils.*;

/**
 * MLProcessor
 */
public class MLProcessor {

    public ObjectMap<Class<?>, RuntimeAnnotationProcessor> annotationProcessors;
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

    public void loadAnnotation(Class<?> clazz) {
        try {
            RuntimeAnnotationProcessor processor = annotationProcessors.get(clazz);
            if (processor == null) throw new RuntimeException("has no processor");
            Seq<Object> list = resolver.get(clazz, null);
            if (list != null) {
                for (Object obj : list) {
                    processor.process(obj);
                }
            }
        } catch (Throwable error) {
            Log.err("process annotation @ error @", clazz.toString(), error.toString());
        }
    }

    public void loadML() {
        for (Class<?> clazz : annotationProcessors.keys()) {
            loadAnnotation(clazz);
        }
    }
}
