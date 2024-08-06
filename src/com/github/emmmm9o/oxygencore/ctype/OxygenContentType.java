package com.github.emmmm9o.oxygencore.ctype;

import com.github.emmmm9o.oxygencore.io.IOPortType;

import arc.util.Nullable;

public class OxygenContentType {
  public int id;
  public final String name;
  public final @Nullable Class<? extends OxygenContent> contentClass;

  public OxygenContentType(String name, Class<? extends OxygenContent> contentClass) {
    this.name = name;
    this.contentClass = contentClass;
  }

  public static OxygenContentType io_port = new OxygenContentType("io_port", IOPortType.class);
}
