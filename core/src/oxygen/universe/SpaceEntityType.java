/* (C) 2025 */
package oxygen.universe;

import java.lang.reflect.*;

import arc.func.*;
import arc.util.*;
import mindustry.mod.Mods.*;
import oxygen.ctype.*;
import oxygen.graphics.universe.*;

public class SpaceEntityType extends OCDContent {
  public Prov<SpaceEntity> entityType = null;
  public @Nullable Class<?> subclass;

  public SpaceEntityType(LoadedMod mod, String name) {
    super(mod, name);
    initType();
  }

  @Override
  public String getContentType() {
    return "space_entity";
  }

  public final SpaceEntity newEntity() {
    return entityType.get();
  }

  public void render(SpaceEntity entity, UniverseParams params) {

  }

  public void drawIcon(SpaceEntity entity, UniverseParams params) {

  }

  protected void initType() {
    try {
      Class<?> current = getClass();
      if (current.isAnonymousClass()) {
        current = current.getSuperclass();
      }
      subclass = current;
      while (entityType == null && SpaceEntityType.class.isAssignableFrom(current)) {
        Class<?> type = Structs.find(current.getDeclaredClasses(),
            t -> SpaceEntity.class.isAssignableFrom(t) && !t.isInterface());
        if (type != null) {
          Constructor<? extends SpaceEntity> cons = (Constructor<? extends SpaceEntity>) type
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
      throw new ArcRuntimeException("SpaceEntity " + getClass() + "no entityType");
    }
  }
}
