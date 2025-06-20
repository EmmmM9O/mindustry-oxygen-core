/* (C) 2025 */
package oxygen.universe;

import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import oxygen.graphics.universe.*;

public abstract class SpaceEntity {
  public @Nullable Universe universe;
  public @Nullable SpaceEntity father, solar;
  public Seq<SpaceEntity> children = new Seq<>();
  public Vec3 position = new Vec3();
  public SpaceEntityType type;
  public float lengthScale = 1e6f, massScale = 1e4f;
  public OrbitEntity orbit;

  public boolean isIcon(UniverseParams params) {
    return false;
  }

  public void render(UniverseParams params) {
    type.render(this, params);
  }

  public void drawIcon(UniverseParams params) {
    type.drawIcon(this, params);
  }

  public void updateSelf(float delta) {
    orbit.update(delta);
  }

  public void update(float delta) {
    updateSelf(delta);
    for (SpaceEntity child : children) {
      child.update(delta);
    }
  }

  public SpaceEntity getSolar() {
    if (solar != null)
      return solar;
    var cur = father;
    while (cur != null) {
      solar = cur;
      cur = cur.father;
    }
    return solar;
  }

  public void init() {
    getSolar();
  }

  public void display(Table table) {

  }

}
