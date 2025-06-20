package oxygen.universe.orbit;

import arc.math.geom.*;
import arc.util.io.*;
import mindustry.mod.Mods.*;
import oxygen.graphics.universe.*;
import oxygen.universe.*;
import oxygen.util.*;

public class FixedOrbit extends OrbitType {
  public FixedOrbit(LoadedMod mod, String name) {
    super(mod, name);
  }

  public static class FixedOrbitEntity extends OrbitEntity {
    public Vec3 pos;

    @Override
    public void update(float delta) {

    }

    @Override
    public void draw(UniverseParams params) {

    }

    @Override
    public void read(Reads reads) {
      pos = IOUtil.vec3(reads);
    }

    @Override
    public void write(Writes writes) {
      IOUtil.vec3(writes, pos);
    }
  }
}
