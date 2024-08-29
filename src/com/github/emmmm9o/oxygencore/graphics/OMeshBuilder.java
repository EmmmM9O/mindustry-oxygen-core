package com.github.emmmm9o.oxygencore.graphics;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import arc.graphics.Mesh;
import arc.graphics.VertexAttribute;
import arc.math.geom.Vec3;
import arc.util.Log;

/**
 * OMeshBuilder
 */
public class OMeshBuilder {

  private static Mesh mesh;

  public static Mesh buildIcosphere(int divisions) {
    int numVertices = (divisions + 1) * (divisions + 1);
    int numIndices = divisions * divisions * 6;
    mesh = new Mesh(true, numVertices * 8, numIndices * 2,
        VertexAttribute.position3,
        VertexAttribute.normal,
        VertexAttribute.texCoords);
    mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
    mesh.getVerticesBuffer().position(0);

    mesh.getIndicesBuffer().limit(mesh.getMaxIndices());
    mesh.getIndicesBuffer().position(0);

    for (int lat = 0; lat <= divisions; lat++) {
      float theta = lat * (float) Math.PI / divisions;
      float sinTheta = (float) Math.sin(theta);
      float cosTheta = (float) Math.cos(theta);
      for (int lon = 0; lon <= divisions; lon++) {
        float phi = lon * 2.0f * (float) Math.PI / divisions;
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        float x = cosPhi * sinTheta * -1;
        float y = cosTheta * -1;
        float z = sinPhi * sinTheta * -1;
        float u = 1f-(float) lon / divisions;
        float v = 1f-(float) lat / divisions;
        mesh.getVerticesBuffer().put(x);
        mesh.getVerticesBuffer().put(y);
        mesh.getVerticesBuffer().put(z);
        mesh.getVerticesBuffer().put(x);
        mesh.getVerticesBuffer().put(y);
        mesh.getVerticesBuffer().put(z);
        mesh.getVerticesBuffer().put(u);
        mesh.getVerticesBuffer().put(v);
        if (lat < divisions && lon < divisions) {
          int cur = lat * (divisions + 1) + lon;
          int next = cur + divisions + 1;

          mesh.getIndicesBuffer().put((short) cur);
          mesh.getIndicesBuffer().put((short) next);
          mesh.getIndicesBuffer().put((short) (cur + 1));
          mesh.getIndicesBuffer().put((short) (cur + 1));
          mesh.getIndicesBuffer().put((short) next);
          mesh.getIndicesBuffer().put((short) (next + 1));
        }
      }
    }

    return mesh;
  }
}
