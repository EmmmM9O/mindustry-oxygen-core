/* (C) 2024 */
package oxygen.utils;

import arc.func.*;

/**
 * EventHandler
 */
public class EventHandler<T> implements Comparable<EventHandler<?>> {
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

  @Override
  public int compareTo(EventHandler<?> other) {
    return this.priority - other.priority;
  }
}
