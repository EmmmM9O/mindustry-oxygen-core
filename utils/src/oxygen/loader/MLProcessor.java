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
    public AMarkResolver resolver;

    public MLProcessor() {
        resolver = new AMarkResolver();
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

    public <T> void loadAnnotation(Class<T> clazz, Cons<Object> func) {
        try {
            Seq<Object> list = resolver.get(clazz, null);
            if (list != null) {
                for (Object obj : list) {
                    func.get(obj);
                }
            }
        } catch (Throwable error) {
            Log.err("process annotation @ error @", clazz.toString(), error.toString());
        }
    }

    public void loadML() {
        loadAnnotation(ML.Instance.class, obj -> {
            if (obj instanceof Field field) {

                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(field.toString() + " must be static");
                }
                field.setAccessible(true);
                ML.Instance annotation = field.getAnnotation(ML.Instance.class);
                if (annotation == null) {
                    throw new RuntimeException(obj.toString() + " marks.json mark it but it do not");
                }
                try {
                    field.set(null, Vars.mods.getMod(annotation.value()));
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
            }
	    else throw new RuntimeException(obj.toString() + " only support FIELD");
        });
    }
}
