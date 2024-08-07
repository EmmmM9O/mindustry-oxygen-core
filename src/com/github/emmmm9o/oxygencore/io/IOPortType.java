package com.github.emmmm9o.oxygencore.io;

import java.lang.reflect.Constructor;

import com.github.emmmm9o.oxygencore.blocks.IOBlock.IOBuild;
import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;

import arc.func.Func2;
import arc.util.Nullable;
import arc.util.Structs;
import arc.util.io.Reads;
import mindustry.mod.Mods.LoadedMod;

public abstract class IOPortType extends OxygenInfoContent {
  public Func2<IOBuild, Integer, IOPort> portType;
  public @Nullable Class<?> subclass;

  public IOPortType(String name, LoadedMod mod) {
    super(name, mod);
    initPort();
  }

  /* Do not use this unless you are work for the oxygen core */
  public IOPortType(String name) {
    this(name, Manager.mod);
  }

  @Override
  public OxygenContentType getContentType() {
    return OxygenContentType.io_port;
  }

  public String getDisplayName() {
    return localizedName;
  }

  public IOPort create(IOBuild build, int index) {
    return portType.get(build, index);
  }

  public IOPort readFrom(Reads read, IOBuild build, int index) {
    var r = create(build, index);
    r.read(read);
    return r;
  }

  public void initPort() {
    try {
      Class<?> current = getClass();

      if (current.isAnonymousClass()) {
        current = current.getSuperclass();
      }

      subclass = current;
      while (portType == null && IOPortType.class.isAssignableFrom(current)) {
        Class<?> type = Structs.find(
            current.getDeclaredClasses(), t -> IOPort.class.isAssignableFrom(t) && !t.isInterface());
        if (type != null) {
          Constructor<? extends IOPort> cons = (Constructor<? extends IOPort>) type
              .getDeclaredConstructor(type.getDeclaringClass());
          portType = (a, b) -> {
            try {
              return cons.newInstance(a, b, this);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };

        }
        current = current.getSuperclass();
      }

    } catch (Throwable ignored) {

    }
    if (portType == null) {
      portType = (a, b) -> null;
      // only for debug
      throw new RuntimeException("Error class " + getClass().toString());
    }
  }
}
