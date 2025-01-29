/* (C) 2024 */
package oxygen.utils;

import arc.func.*;
import arc.struct.*;
import arc.util.*;

/**
 * OEvents
 */
@SuppressWarnings("unchecked")
public class OPQEvent implements OEvent {
  public ObjectMap<String, PQueue<EventHandler<?>>> events;
  public String classPrefix = "class:";
  public String enumPrefix = "enum:";

  @Override
  public Seq<String> getKeys() {
    return events.keys().toSeq();
  }

  @Override
  public Seq<EventHandler<?>> getValues(String key) {
    return Seq.with(getQueue(key).queue).as();
  }

  @Override
  public boolean isEnumKey(String key) {
    return key.startsWith(enumPrefix);
  }

  @Override
  public boolean isClassKey(String key) {
    return key.startsWith(classPrefix);
  }

  @Override
  public String getClassFromKey(String key) {
    return key.substring(classPrefix.length());
  }

  @Override
  public void on(String type, Func<?, Boolean> listener, int priority) {
    getQueue(type).add(new EventHandler<>(listener, priority));
  }

  @Override
  public String getEnumPre(String type) {
    return enumPrefix + type;
  }

  @Override
  public String getEnumKey(Object type) {
    return getEnumPre(type.getClass().getName() + "." + type.toString());
  }

  @Override
  public String getClassPre(String type) {
    return classPrefix + type;
  }

  @Override
  public String getClassKey(Class<?> type) {
    return getClassPre(type.getName());
  }

  @Override
  public void run(String type, Prov<Boolean> listener, int priority) {
    getQueue(type).add(new EventHandler<>(listener, priority));
  }

  public PQueue<EventHandler<?>> getQueue(String type) {
    return events.get(type, PQueue::new);
  }

  @Override
  public void mark(String type) {
    getQueue(type);
  }

  @Override
  public boolean isMarked(String key) {
    return events.containsKey(key);
  }

  @Override
  public void fire(String type, Object obj) {
    PQueue<EventHandler<?>> queue = events.get(type);
    if (queue != null) {
      boolean flag = false;
      for (Object objr : queue.queue) {
        if (objr instanceof EventHandler<?> top) {
          if (!flag) {
            try {
              flag = ((Func<Object, Boolean>) top.func).get(obj);
            } catch (Throwable error) {
              Log.err("OEvent error @ : @", type, error.toString());
            }
          }
        }
      }
    }
  }

  public OPQEvent() {
    events = new ObjectMap<>();
  }
}
