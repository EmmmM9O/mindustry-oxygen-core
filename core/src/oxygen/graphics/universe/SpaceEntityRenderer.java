/* (C) 2025 */
package oxygen.graphics.universe;

import oxygen.universe.*;

public interface SpaceEntityRenderer<T extends SpaceEntityType, E extends SpaceEntity> {
  public void draw(T type, E entity, UniverseParams params);
}
