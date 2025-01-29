/* (C) 2024 */
package oxygen.utils;

import arc.*;
import arc.func.*;
import arc.struct.*;
import mindustry.game.*;

/**
 * OEvent
 */
public interface OEvent {
  public void on(String type, Func<?, Boolean> listener, int priority);

  public default <T> void on(Class<T> type, Func<T, Boolean> listener, int priority) {
    on(getClassKey(type), listener, priority);
  }

  public void run(String type, Prov<Boolean> listener, int priority);

  public default void run(Object type, Prov<Boolean> listener, int priority) {
    run(getEnumKey(type), listener, priority);
  }

  public boolean isClassKey(String key);

  public boolean isEnumKey(String key);

  public Seq<String> getKeys();

  public Seq<EventHandler<?>> getValues(String key);

  public String getClassKey(Class<?> type);

  public String getEnumKey(Object type);

  public String getClassPre(String type);

  public String getEnumPre(String type);

  public String getClassFromKey(String key);

  public void mark(String type);

  public boolean isMarked(String key);

  public default void mark(Class<?> type) {
    mark(getClassKey(type));
  }

  public default void markEnum(Object type) {
    mark(getEnumKey(type));
  }

  public void fire(String type, Object obj);

  public default <T> void fire(T obj) {
    fire(obj.getClass(), obj);
  }

  public default void fireEnum(Object type) {
    fire(getEnumKey(type), type);
  }

  public default void fire(Class<?> type, Object obj) {
    fire(getClassKey(type), obj);
  }

  public default void markAll(Class<?> clazz) {
    if (clazz.isEnum()) {
      for (Object obj : clazz.getEnumConstants()) {
        markEnum(obj);
      }
    } else if (clazz.getDeclaredFields().length != 0) {
      mark(clazz);
    }
    for (Class<?> subclass : clazz.getDeclaredClasses()) {
      markAll(subclass);
    }
  }

  public default void putEvents() {
    for (Class<?> subclass : EventType.class.getDeclaredClasses()) {
      if (subclass.isEnum()) {
        for (Object obj : subclass.getEnumConstants()) {
          markEnum(obj);
          Events.run(obj, () -> {
            fireEnum(obj);
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
}
