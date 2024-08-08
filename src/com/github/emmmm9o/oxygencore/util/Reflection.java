package com.github.emmmm9o.oxygencore.util;

import arc.util.Log;

/**
 * Reflection
 */
public class Reflection {

  public static <T> T construct(Class<T> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Throwable err) {
      Log.err("reflection err @", err);
    }
    return null;
  }
  public static <T> T callCreate(Class<T> clazz) {
    try {
      return (T)clazz.getDeclaredMethod("create").invoke(null);
    } catch (Throwable err) {
      Log.err("reflection err @", err);
    }
    return null;
  }
}
