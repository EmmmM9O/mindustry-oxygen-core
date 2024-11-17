/* (C) 2024 */
package oxygen.utils;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import java.util.*;
import mindustry.game.*;

/**
 * OEvents
 */
@SuppressWarnings("unchecked")
public class OEvents {
    public static class EventHandler<T> {
        public Func<T, Boolean> func;
        public int priority;

        public EventHandler(Func<T, Boolean> func, int priority) {
            this.func = func;
            this.priority = priority;
        }

        public EventHandler(Prov<Boolean> func, int priority) {
            this.func = (t) -> func.get();
            this.priority = priority;
        }
    }

    public static Comparator<EventHandler<?>> standardPriorityComparator = Comparator.comparingInt(obj -> obj.priority);
    public Comparator<EventHandler<?>> priorityComparator;
    public ObjectMap<String, PQueue<EventHandler<?>>> events;

    public void on(String type, Func<?, Boolean> listener, int priority) {
        events.get(type, () -> new PQueue<>(12, priorityComparator)).add(new EventHandler<>(listener, priority));
    }

    public <T> void on(Class<T> type, Func<T, Boolean> listener, int priority) {
        on(type.getName(), listener, priority);
    }

    public String getName(Object type) {
        return type.getClass().getName() + "." + type.toString();
    }

    public void run(Object type, Prov<Boolean> listener, int priority) {
        run(getName(type), listener, priority);
    }

    public void run(String type, Prov<Boolean> listener, int priority) {
        events.get(type, () -> new PQueue<>(12, priorityComparator)).add(new EventHandler<>(listener, priority));
    }

    public void mark(String type) {
        events.get(type, () -> new PQueue<>(12, priorityComparator));
    }

    public void mark(Class<?> type) {
        mark(type.getName());
    }

    public void markEnum(Object type) {
        mark(getName(type));
    }

    public void fire(String type, Object obj) {
        PQueue<EventHandler<?>> queue = events.get(type);
        if (queue != null) {
            PQueue<EventHandler<?>> tmp = new PQueue<>(12, priorityComparator);
            boolean flag = false;
            while (!queue.empty()) {
                EventHandler<?> top = queue.poll();
                tmp.add(top);
                if (!flag) {
                    try {
                        flag = ((Func<Object, Boolean>) top.func).get(obj);
                    } catch (Throwable error) {
                        Log.err("OEvents error @ : @", type, error.toString());
                    }
                }
            }
            events.put(type, tmp);
        }
    }

    public void fireRun(Object type) {
        fire(getName(type), type);
    }

    public <T> void fire(Class<?> type, T obj) {
        fire(type.getName(), obj);
    }

    public <T> void fire(T obj) {
        fire(obj.getClass(), obj);
    }

    public void putEvents() {
        for (Class<?> subclass : EventType.class.getDeclaredClasses()) {
            if (subclass.isEnum()) {
                for (Object obj : subclass.getEnumConstants()) {
                    markEnum(obj);
                    Events.run(obj, () -> {
                        fireRun(obj);
                    });
                }
                continue;
            }
            mark(subclass);
            Events.on(subclass, event -> {
                fire(subclass, event);
            });
        }
    }

    public OEvents() {
        priorityComparator = standardPriorityComparator;
        events = new ObjectMap<>();
    }
}
