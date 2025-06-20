package oxygen.universe;

import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import oxygen.graphics.universe.*;

public abstract class OrbitEntity {
  public SpaceEntity entity;

  public void init(SpaceEntity entity) {
    this.entity = entity;
  }

  public abstract void draw(UniverseParams params);

  // public abstract void display(Table table);

  public abstract void update(float delta);

  public abstract void write(Writes writes);

  public abstract void read(Reads reads);
}
