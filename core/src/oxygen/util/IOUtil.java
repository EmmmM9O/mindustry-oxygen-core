package oxygen.util;

import arc.math.geom.*;
import arc.util.io.*;

public class IOUtil {
  public static Vec3 vec3(Reads reads) {
    return new Vec3(reads.f(), reads.f(), reads.f());
  }

  public static void vec3(Writes writes, Vec3 v) {
    writes.f(v.x);
    writes.f(v.y);
    writes.f(v.z);
  }
}
