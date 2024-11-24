/* (C) 2024 */
package oxygen.loader;

import arc.struct.*;
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
            public String resolve(Event annotation, Method method);
        }
        public static EventResolver idResolver=(event,method)->{
            if(!event.event().isEmpty()){
                return event.event();
            }
            return null;
        };
        public Seq<EventResolver> resolvers;
        public OEvents events;

        public EventAnnotatonProcessor(OEvents events) {
            super(Event.class, true);
            resolvers = Seq.with();
            this.events = events;
        }

        public void standardResolvers() {}

        @Override
        public void process(Event annotation, Method value) throws Throwable {
            for (EventResolver processor : resolvers) {
                String res = processor.resolve(annotation, value);
                if (res == null || res.isEmpty()) continue;
                events.on(
                        res,
                        obj -> {
                            return true;
                        },
                        annotation.value());
                return;
            }
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
