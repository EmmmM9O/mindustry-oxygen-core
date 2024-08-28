package com.github.emmmm9o.oxygencore.graphics;

import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.VertexAttribute;
import arc.math.Mathf;
import arc.math.geom.Icosphere;
import arc.math.geom.MeshResult;
import arc.math.geom.Vec3;

/**
 * OMeshBuilder
 */
public class OMeshBuilder {

  private static final Vec3 v1 = new Vec3(), v2 = new Vec3(), v3 = new Vec3(), v4 = new Vec3();
  private static final float[] floats = new float[3 + 3 + 2];
  private static Mesh mesh;

  public static Mesh buildIcosphere(int divisions, float radius) {
    begin(20 * (2 << (2 * divisions - 1)) * 8 * 3);

    MeshResult result = Icosphere.create(divisions);
    for (int i = 0; i < result.indices.size; i += 3) {
      v1.set(result.vertices.items, result.indices.items[i] * 3);
      v2.set(result.vertices.items, result.indices.items[i + 1] * 3);
      v3.set(result.vertices.items, result.indices.items[i + 2] * 3);
      verts(v1, v3, v2, normal(v1, v2, v3).scl(-1f), radius);
    }

    return end();
  }

  private static void begin(int count) {
    mesh = new Mesh(true, count, 0,
        VertexAttribute.position3,
        VertexAttribute.normal,
        VertexAttribute.texCoords);

    mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
    mesh.getVerticesBuffer().position(0);
  }

  private static Mesh end() {
    Mesh last = mesh;
    last.getVerticesBuffer().limit(last.getVerticesBuffer().position());
    mesh = null;
    return last;
  }

  private static Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3) {
    return v4.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
  }

  private static void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal, float radius) {
    vert(a, normal, radius);
    vert(b, normal, radius);
    vert(c, normal, radius);
  }

  private static void vert(Vec3 a, Vec3 normal, float radius) {
    floats[0] = a.x;
    floats[1] = a.y;
    floats[2] = a.z;

    floats[3] = normal.x;
    floats[4] = normal.y;
    floats[5] = normal.z;
    floats[6] = (float) ((Math.atan2(a.z, a.x) + Math.PI) / (2d * Math.PI));
    if (floats[6] <= 0)
      floats[6] = 0;
    if (floats[6] >= 0.95)
      floats[6] = 1;
    floats[7] = 1f-(a.y + 1f) / (2f);

    mesh.getVerticesBuffer().put(floats);
  }
}
