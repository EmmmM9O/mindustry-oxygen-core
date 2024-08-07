package com.github.emmmm9o.oxygencore.util;

import arc.util.Log;

/**
 * Reflection
 */
public class Reflection {

  public static <T> T construct(Class<T> clazz) {
    try {
      return clazz.getConstructor().newInstance();
    } catch (Throwable err) {
      Log.err("reflection err @", err);
    }
    return null;
  }
}
