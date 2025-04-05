/* (C) 2025 */
package oxygen.universe;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import oxygen.graphics.universe.*;

public abstract class SpaceEntity {
  public @Nullable SpaceEntity father;
  public Seq<SpaceEntity> children = new Seq<>();
  public Vec3 position = new Vec3();
  public SpaceEntityType type;
  public float lengthScale = 1e6f, massScale = 1e4f;

  public boolean isIcon(UniverseParams params) {
    return false;
  }

  public void render(UniverseParams params) {
    type.render(this, params);
  }

  public void drawIcon(UniverseParams params) {
    type.drawIcon(this, params);
  }

  public void update() {
    for (SpaceEntity child : children) {
      child.update();
    }
  }
}
