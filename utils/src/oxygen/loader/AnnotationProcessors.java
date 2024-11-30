/* (C) 2024 */
package oxygen.loader;

import arc.struct.*;
import arc.util.Log;
import java.lang.annotation.*;
import java.lang.reflect.*;
import mindustry.*;
import oxygen.loader.ML.*;
import oxygen.utils.OEvents;

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

        public void process(T annotation, Method value) throws Throwable {
        }

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

        public void process(T annotation, Field value) throws Throwable {
        }

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

    public static class InstanceAnnotationProcessor extends FieldAnnotationProcessor<Instance> {
        public InstanceAnnotationProcessor() {
            super(Instance.class, true);
        }

        @Override
        public void process(Instance annotation, Field field) throws Throwable {
            field.setAccessible(true);
            field.set(null, Vars.mods.getMod(annotation.value()));
        }
    }

    public class EventAnnotatonProcessor extends MethodAnnotationProcessor<Event> {
        public static interface EventResolver {
            public String resolve(Event annotation, Method method, EventAnnotatonProcessor self);
        }

        public static interface EventExecutor {
            public Boolean execute(Event annotation, Method method, Object eve, EventAnnotatonProcessor self,
                    String res);
        }

        public static EventResolver idResolver = new EventResolver() {
            public String resolve(Event event, Method method, EventAnnotatonProcessor self) {
                if (!event.event().isEmpty()) {
                    return event.event();
                }
                return null;
            }
        },
                idEventResolver = new EventResolver() {
                    public String resolve(Event event, Method method, EventAnnotatonProcessor self) {
                        if (method.getParameterCount() != 0) {
                            return null;
                        }
                        Seq<String> arr = Seq.with();
                        for (String tar : self.events.events.keys()) {
                            if (tar.indexOf(event.event()) != -1) {
                                arr.add(tar);
                            }
                        }
                        if (arr.size == 0) {
                            return null;
                        }
                        if (arr.size != 1) {
                            Seq<String> tmp = Seq.select(arr.toArray(), data -> {
                                return data.indexOf(method.getName()) != -1;
                            });
                            arr = tmp;
                        }
                        if (arr.size == 1) {
                            return arr.get(0);
                        }
                        return null;
                    }
                },
                paramsResolver = new EventResolver() {
                    public String resolve(Event event, Method method, EventAnnotatonProcessor self) {
                        Seq<String> res = Seq.with();
                        if (method.getParameterCount() == 0) {
                            return null;
                        }
                        for (String id : self.events.events.keys()) {
                            if (!id.startsWith("class:")) {
                                continue;
                            }
                            try {
                                Class<?>[] params = method.getParameterTypes();
                                Class<?> clazz = Class.forName(id.substring(6));
                                Field[] fields = clazz.getDeclaredFields();
                                if (fields.length < method.getParameterCount()) {
                                    continue;
                                }
                                boolean flag = false;
                                for (int i = 0; i < method.getParameterCount(); i++) {
                                    if (fields[i].getName() != params[i].getName()) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (flag)
                                    continue;
                                res.add(id);
                                if (!event.event().isEmpty()) {
                                    if (res.size == 0) {
                                        return null;
                                    }
                                    if (res.size != 1) {
                                        Seq<String> tmp = Seq.select(res.toArray(), data -> {
                                            return data.indexOf(event.event()) != -1;
                                        });
                                        res = tmp;
                                    }
                                }
                                if (res.size == 0) {
                                    return null;
                                }
                                if (res.size != 1) {
                                    Seq<String> tmp = Seq.select(res.toArray(), data -> {
                                        return data.indexOf(method.getName()) != -1;
                                    });
                                    res = tmp;
                                }
                            } catch (Throwable err) {
                                Log.err("find class error @", err);
                            }
                        }
                        return null;
                    }
                },
                classResolver = new EventResolver() {
                    public String resolve(Event event, Method method, EventAnnotatonProcessor self) {
                        if (method.getParameterCount() != 1) {
                            return null;
                        }
                        String res = self.events.getClassName(method.getParameterTypes()[0]);
                        return self.events.events.keys().toSeq().indexOf(res) != -1 ? res : null;
                    }
                };
        public static EventExecutor enumExectuor = new EventExecutor() {
            public Boolean execute(Event annotation, Method method, Object eve, EventAnnotatonProcessor self,
                    String res) {
                if (!res.startsWith("enum:") || method.getParameterCount() != 0) {
                    return null;
                }
                Object obj = null;
                try {
                    obj = method.invoke(null);
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
                if (Boolean.class.isAssignableFrom(method.getReturnType())) {
                    return (Boolean) obj;
                }
                return false;
            }
        }, classExectuor = new EventExecutor() {
            public Boolean execute(Event annotation, Method method, Object eve, EventAnnotatonProcessor self,
                    String res) {
                if (!res.startsWith("class:") || method.getParameterCount() != 1
                        || res.indexOf(eve.getClass().getName()) == -1) {
                    return null;
                }
                Object obj = null;
                try {
                    obj = method.invoke(eve);
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
                if (Boolean.class.isAssignableFrom(method.getReturnType())) {
                    return (Boolean) obj;
                }
                return false;
            }
        }, paramasExecutor = new EventExecutor() {
            public Boolean execute(Event annotation, Method method, Object eve, EventAnnotatonProcessor self,
                    String res) {
                if (!res.startsWith("class:") || method.getParameterCount() == 0
                        || eve.getClass().getDeclaredFields().length < method.getParameterCount()) {
                    return null;
                }
                Object obj = null;
                try {
                    Seq<Object> objs = Seq.with();
                    Field[] fields = eve.getClass().getDeclaredFields();
                    for (int i = 0; i < method.getParameterCount(); i++) {
                        objs.add(fields[i].get(eve));
                    }
                    obj = method.invoke(eve, objs.toArray());
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                }
                if (Boolean.class.isAssignableFrom(method.getReturnType())) {
                    return (Boolean) obj;
                }
                return false;
            }
        };
        public Seq<EventResolver> resolvers;
        public Seq<EventExecutor> executors;
        public OEvents events;

        public EventAnnotatonProcessor(OEvents events) {
            super(Event.class, true);
            resolvers = Seq.with();
            executors = Seq.with();
            this.events = events;
            standardResolvers();
        }

        public void standardResolvers() {
            resolvers.addAll(idResolver, paramsResolver, idEventResolver, classResolver);
        }

        public void standardExectuor() {
            executors.addAll(enumExectuor, classExectuor, paramasExecutor);
        }

        @Override
        public void process(Event annotation, Method value) throws Throwable {
            for (EventResolver processor : resolvers) {
                String res = processor.resolve(annotation, value, this);
                if (res == null || res.isEmpty())
                    continue;
                events.on(
                        res,
                        obj -> {
                            for (EventExecutor executor : executors) {
                                Boolean re = executor.execute(annotation, value, obj, this, res);
                                if (re != null)
                                    return re;
                            }
                            return false;
                        },
                        annotation.value());
                return;
            }
            throw new RuntimeException("can not resolve event method");
        }
    }

    public static InstanceAnnotationProcessor instanceProcessor = new InstanceAnnotationProcessor();
    public static ObjectMap<Class<?>, RuntimeAnnotationProcessor> standardProcessors = new ObjectMap<>();

    {
        standardProcessors.put(Instance.class, instanceProcessor);
    }

    public static void setStandardProcessor(MLProcessor processor) {
        processor.annotationProcessors.putAll(standardProcessors);
    }
}
