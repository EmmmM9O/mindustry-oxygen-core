/* (C) 2024 */
package oxygen.loader;

import arc.struct.*;
import arc.util.Log;
import java.lang.annotation.*;
import java.lang.reflect.*;
import mindustry.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import oxygen.loader.ML.*;
import oxygen.loader.MLProcessor.*;
import oxygen.utils.*;

/**
 * AnnotationProcessors
 */
public class AnnotationProcessors {
    public static class MethodAnnotationProcessor<T extends Annotation> implements RuntimeAnnotationProcessor {
        public Class<T> annotationClass;
        public boolean isStatic;

        public MethodAnnotationProcessor(Class<T> aClass, boolean isStatic) {
            this.annotationClass = aClass;
            this.isStatic = isStatic;
        }

        public void process(T annotation, Method value) throws Throwable {}

        @Override
        public void process(Object obj) throws Throwable {
            if (obj instanceof Method method) {
                T annotation = method.getAnnotation(annotationClass);

                if (annotation == null) {
                    throw new RuntimeException(" marks.json mark " + method.getName() + " but it has not");
                }
                if (isStatic && !Modifier.isStatic(method.getModifiers())) {
                    throw new RuntimeException("must be static");
                }
                process(annotation, method);
                return;
            }
            throw new RuntimeException(" must be METHOD");
        }
    }

    public static class FieldAnnotationProcessor<T extends Annotation> implements RuntimeAnnotationProcessor {
        public Class<T> annotationClass;
        public boolean isStatic;

        public FieldAnnotationProcessor(Class<T> aClass, boolean isStatic) {
            this.annotationClass = aClass;
            this.isStatic = isStatic;
        }

        public void process(T annotation, Field value) throws Throwable {}

        @Override
        public void process(Object obj) throws Throwable {
            if (obj instanceof Field field) {
                T annotation = field.getAnnotation(annotationClass);

                if (annotation == null) {
                    throw new RuntimeException(" marks.json mark " + field.getName() + " but it has not");
                }
                if (isStatic && !Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException("must be static");
                }
                process(annotation, field);
                return;
            }
            throw new RuntimeException(" must be FIELD");
        }
    }

    public static class ClassAnnotationProcessor<T extends Annotation> implements RuntimeAnnotationProcessor {
        public Class<T> annotationClass;
        public boolean isStatic;

        public ClassAnnotationProcessor(Class<T> aClass, boolean isStatic) {
            this.annotationClass = aClass;
            this.isStatic = isStatic;
        }

        public void process(T annotation, Class<?> value) throws Throwable {}

        @Override
        public void process(Object obj) throws Throwable {
            if (obj instanceof Class<?> clazz) {
                T annotation = clazz.getAnnotation(annotationClass);

                if (annotation == null) {
                    throw new RuntimeException(" marks.json mark " + clazz.getName() + " but it has not");
                }
                if (isStatic && !Modifier.isStatic(clazz.getModifiers())) {
                    throw new RuntimeException("must be static");
                }
                process(annotation, clazz);
                return;
            }
            throw new RuntimeException(" must be class");
        }
    }

    public static class EventTypeAnnotationProcessor extends ClassAnnotationProcessor<EventType> {
        public OEvent event;

        public EventTypeAnnotationProcessor(OEvent event) {
            super(EventType.class, true);
            this.event = event;
        }

        @Override
        public void process(EventType annotation, Class<?> value) throws Throwable {
            event.markAll(value);
        }
    }

    public static class InstanceAnnotationProcessor extends FieldAnnotationProcessor<Instance> {
        public InstanceAnnotationProcessor() {
            super(Instance.class, true);
        }

        @Override
        public void process(Instance annotation, Field field) throws Throwable {
            field.setAccessible(true);
            if (LoadedMod.class.isAssignableFrom(field.getType()))
                field.set(null, Vars.mods.getMod(annotation.value()));
            if (ModMeta.class.isAssignableFrom(field.getType()))
                field.set(null, Vars.mods.getMod(annotation.value()).meta);
            if (Mod.class.isAssignableFrom(field.getType())) field.set(null, Vars.mods.getMod(annotation.value()).main);
        }
    }

    public static class EventAnnotationProcessor extends MethodAnnotationProcessor<Event> {
        public OEvent event;

        public EventAnnotationProcessor(OEvent event) {
            super(Event.class, true);
            this.event = event;
            filters = Seq.with();
        }

        public static interface EventFilter {
            public Seq<String> filter(
                    Event annotation, Method method, OEvent event, Seq<String> keys, EventAnnotationProcessor self)
                    throws Throwable;
        }

        public static EventFilter
                idFilter =
                        new EventFilter() {
                            public Seq<String> filter(
                                    Event annotation,
                                    Method method,
                                    OEvent event,
                                    Seq<String> keys,
                                    EventAnnotationProcessor self)
                                    throws Throwable {
                                if (annotation.event().isEmpty()) return keys;
                                if (event.isMarked(annotation.event())) return Seq.with(annotation.event());
                                if (event.isMarked(event.getClassPre(annotation.event())))
                                    return Seq.with(event.getClassPre(annotation.event()));
                                if (event.isMarked(event.getEnumPre(annotation.event())))
                                    return Seq.with(event.getEnumPre(annotation.event()));
                                return keys.select(str -> str.contains(annotation.event()));
                            }
                        },
                dirClassFilter =
                        new EventFilter() {
                            public Seq<String> filter(
                                    Event annotation,
                                    Method method,
                                    OEvent event,
                                    Seq<String> keys,
                                    EventAnnotationProcessor self)
                                    throws Throwable {
                                if (method.getParameterCount() == 1) {
                                    Class<?> type = method.getParameterTypes()[0];
                                    if (event.isMarked(event.getClassKey(type))) {
                                        return Seq.with(event.getClassKey(type));
                                    }
                                }
                                return keys;
                            }
                            ;
                        },
                paramsFilter =
                        new EventFilter() {
                            public Seq<String> filter(
                                    Event annotation,
                                    Method method,
                                    OEvent event,
                                    Seq<String> keys,
                                    EventAnnotationProcessor self)
                                    throws Throwable {
                                return keys.select(str -> {
                                    if (event.isEnumKey(str) && method.getParameterCount() == 0) {
                                        return true;
                                    }
                                    if (event.isClassKey(str)) {
                                        try {
                                            Class<?> type = Class.forName(event.getClassFromKey(str));
                                            Field[] fields = type.getDeclaredFields();
                                            if (fields.length < method.getParameterCount()) {
                                                return false;
                                            }
                                            Class<?>[] params = method.getParameterTypes();
                                            for (int i = 0; i < method.getParameterCount(); i++) {
                                                if (params[i] != fields[i].getType()) return false;
                                            }
                                            return true;
                                        } catch (Throwable err) {
                                            throw new RuntimeException(err);
                                        }
                                    }
                                    return false;
                                });
                            }
                        },
                nameFilter =
                        new EventFilter() {
                            public Seq<String> filter(
                                    Event annotation,
                                    Method method,
                                    OEvent event,
                                    Seq<String> keys,
                                    EventAnnotationProcessor self)
                                    throws Throwable {
                                return keys.select(str -> str.contains(method.getName()));
                            }
                        };

        public static interface EventExecutor {
            public Boolean execute(String res, Method method, OEvent event, Object obj, EventAnnotationProcessor self);
        }

        public static EventExecutor baseExectuor = new EventExecutor() {
            public boolean invoke(Method method, Object[] objs) throws Throwable {
                Object res = method.invoke(null, objs);
                if (Boolean.class.isAssignableFrom(method.getReturnType())) {
                    return (Boolean) (res);
                }
                return false;
            }

            public Boolean execute(String res, Method method, OEvent event, Object obj, EventAnnotationProcessor self) {
                try {
                    if (event.isEnumKey(res)) {
                        return invoke(method, new Object[] {});
                    }
                    if (event.isClassKey(res)) {
                        if (method.getParameterCount() == 1
                                && event.getClassFromKey(res) == method.getParameterTypes()[0].getName()) {
                            return invoke(method, new Object[] {obj});
                        }
                        Class<?> type = Class.forName(event.getClassFromKey(res));
                        Field[] fields = type.getDeclaredFields();
                        if (fields.length >= method.getParameterCount()) {
                            boolean flag = true;
                            Class<?>[] params = method.getParameterTypes();
                            for (int i = 0; i < method.getParameterCount(); i++) {
                                if (params[i] != fields[i].getType()) flag = false;
                            }
                            if (flag) {
                                Seq<Object> objs = Seq.with();
                                for (int i = 0; i < method.getParameterCount(); i++) {
                                    objs.add(fields[i].get(obj));
                                }
                                return invoke(method, objs.toArray());
                            }
                        }
                    }
                    return false;
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
            }
        };
        public EventExecutor executor;
        public Seq<EventFilter> filters;

        @Override
        public void process(Event annotation, Method method) throws Throwable {
            Seq<String> keys = event.getKeys();
            for (EventFilter filter : filters) {
                try {
                    keys = filter.filter(annotation, method, event, keys, this);
                    if (keys.size == 0) {
                        throw new RuntimeException("no such event");
                    }
                    if (keys.size == 1) {
                        String key = keys.get(0);
                        event.on(
                                key,
                                obj -> {
                                    if (executor != null) {
                                        return executor.execute(key, method, event, obj, this);
                                    }
                                    return false;
                                },
                                annotation.value());
                        return;
                    }
                } catch (Throwable err) {
                    Log.err("error @", err);
                }
            }
            throw new RuntimeException("no such event");
        }

        public void standardProcessors() {
            filters = Seq.with(dirClassFilter, idFilter, paramsFilter, nameFilter);
            executor = baseExectuor;
        }
    }

    public static InstanceAnnotationProcessor instanceProcessor = new InstanceAnnotationProcessor();
    public static ObjectMap<Key, RuntimeAnnotationProcessor> standardProcessors = new ObjectMap<>();

    {
        standardProcessors.put(new Key(Instance.class, 1), instanceProcessor);
    }

    public static void setStandardProcessor(MLProcessor processor) {
        processor.annotationProcessors.putAll(standardProcessors);
    }

    public static void setEventProcessor(MLProcessor processor, OEvent event) {
        EventAnnotationProcessor process = new EventAnnotationProcessor(event);
        process.standardProcessors();
        processor.annotationProcessors.put(new Key(Event.class, 10000), process);
        processor.annotationProcessors.put(new Key(EventType.class, 1), new EventTypeAnnotationProcessor(event));
    }
}
