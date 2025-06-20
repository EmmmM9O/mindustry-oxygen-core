package oxygen.universe.orbit;

import arc.math.geom.Vec3;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.mod.Mods.*;
import oxygen.graphics.universe.UniverseParams;
import oxygen.universe.*;

public class KeplerianEOrbit extends OrbitType {
  public KeplerianEOrbit(LoadedMod mod, String name) {
    super(mod, name);
  }

  public static class KeplerianEOrbitEntity extends OrbitEntity {
    public double semiMajorAxis;
    public double eccentricity;
    public double inclination;
    public double argumentOfPeriapsis;
    public double longitudeOfAscendingNode;
    public double trueAnomaly;

    /*
     * a = Semi-major axis = size 半长轴
     * e = Eccentricity = shape 离心率
     * i = inclination = tilt 轨道倾角
     * ω = argument of periapsis = twist 近地点幅角
     * Ω = longitude of the ascending node = pin 升交点赤经
     * v = true anomaly = angle now 真近点角
     */

    @Override
    public void update(float delta) {

    }

    @Override
    public void draw(UniverseParams params) {

    }

    @Override
    public void write(Writes writes) {
      writes.d(semiMajorAxis);
      writes.d(eccentricity);
      writes.d(inclination);
      writes.d(argumentOfPeriapsis);
      writes.d(longitudeOfAscendingNode);
      writes.d(trueAnomaly);
    }

    @Override
    public void read(Reads reads) {
      semiMajorAxis = reads.d();
      eccentricity = reads.d();
      inclination = reads.d();
      argumentOfPeriapsis = reads.d();
      longitudeOfAscendingNode = reads.d();
      trueAnomaly = reads.d();
    }
  };
}
