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
    public static interface AnnotationProcessor {
        public void process(Object obj, MLProcessor self);
    }

    public ObjectMap<Class<?>, AnnotationProcessor> annotationProcessors;
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
            AnnotationProcessor processor = annotationProcessors.get(clazz);
            if (processor == null) throw new RuntimeException("has no processor");
            Seq<Object> list = resolver.get(clazz, null);
            if (list != null) {
                for (Object obj : list) {
                    processor.process(obj, this);
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

    public static AnnotationProcessor instanceProcessor = new AnnotationProcessor() {
        public Object getValue(ML.Instance annotation) {
            return Vars.mods.getMod(annotation.value());
        }

        public void process(Object obj, MLProcessor self) {
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
                    field.set(null, getValue(annotation));
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
            } else throw new RuntimeException(obj.toString() + " only support FIELD");
        }
        ;
    };

    public static class EventProcessor implements AnnotationProcessor {
        public OEvents events;
        public Seq<Func3<Method, MLProcessor, ML.Event, Boolean>> methodProcessor;

        public EventProcessor(OEvents events) {
            methodProcessor = Seq.with((method, self, annotation) -> {
                if (!annotation.event().isEmpty()) {
                    events.on(
                            annotation.event(),
                            o -> {
                                return false;
                            },
                            annotation.value());
                    return true;
                }
                return false;
            });
            this.events = events;
        }

        public void runMethod(Method method, MLProcessor self, ML.Event annotation) {
            boolean flag=false;
            for (Func3<Method, MLProcessor, ML.Event, Boolean> func : methodProcessor) {
                if (func.get(method, self, annotation)) {flag=true;break;}
            }
            if(!flag){
                throw new RuntimeException("unknown Event to handle "+annotation);
            }
        }

        public void process(Object obj, MLProcessor self) {
            if (obj instanceof Method method) {

                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new RuntimeException(method.toString() + " must be static");
                }
                method.setAccessible(true);
                ML.Event annotation = method.getAnnotation(ML.Event.class);
                if (annotation == null) {
                    throw new RuntimeException(obj.toString() + " marks.json mark it but it do not");
                }
                try {
                    runMethod(method, self, annotation);
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
            } else throw new RuntimeException(obj.toString() + " only support Method");
        }
    }
    ;

    public void standardProcessors() {
        annotationProcessors.put(ML.Instance.class, instanceProcessor);
    }
}
