package oxygen.universe;

import java.lang.reflect.*;
import mindustry.mod.Mods.*;
import oxygen.ctype.*;

import arc.func.*;
import arc.util.*;

public abstract class OrbitType extends OCDContent {
  public OrbitType(LoadedMod mod, String name) {
    super(mod, name);
    initType();
  }

  @Override
  public String getContentType() {
    return "orbit";
  }

  public Prov<OrbitEntity> entityType = null;
  public @Nullable Class<?> subclass;

  protected void initType() {
    try {
      Class<?> current = getClass();
      if (current.isAnonymousClass()) {
        current = current.getSuperclass();
      }
      subclass = current;
      while (entityType == null && OrbitType.class.isAssignableFrom(current)) {
        Class<?> type = Structs.find(current.getDeclaredClasses(),
            t -> OrbitEntity.class.isAssignableFrom(t) && !t.isInterface());
        if (type != null) {
          Constructor<? extends OrbitEntity> cons = (Constructor<? extends OrbitEntity>) type
              .getDeclaredConstructor(type.getDeclaringClass());
          entityType = () -> {
            try {
              return cons.newInstance(this);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };
        }
      }
    } catch (Throwable ignored) {
    }
    if (entityType == null) {
      throw new ArcRuntimeException("OrbitEntity" + getClass() + "no entityType");
    }
  }

}
