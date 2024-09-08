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

  public static Mesh ring(int divisions, float innerRadius, float outerRadius) {
    int numVertices = divisions * 4;
    int numIndices = divisions * 6;
    mesh = new Mesh(true, numVertices * 8, numIndices * 2,
        VertexAttribute.position3,
        VertexAttribute.normal,
        VertexAttribute.texCoords);
    mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
    mesh.getVerticesBuffer().position(0);

    mesh.getIndicesBuffer().limit(mesh.getMaxIndices());
    mesh.getIndicesBuffer().position(0);
    float angleStep = (float) (2 * Math.PI / divisions);
    for (int i = 0; i < divisions; i++) {
      float angle0 = i * angleStep;
      float angle1 = (i + 1) * angleStep;
      addRingVertex(i, angle0, innerRadius, false);
      mesh.getVerticesBuffer().put(0f);
      mesh.getVerticesBuffer().put(0f);
      addRingVertex(i, angle1, innerRadius, false);
      mesh.getVerticesBuffer().put(0f);
      mesh.getVerticesBuffer().put(1f);
      addRingVertex(i, angle0, outerRadius, false);
      mesh.getVerticesBuffer().put(1f);
      mesh.getVerticesBuffer().put(0f);
      addRingVertex(i, angle1, outerRadius, false);
      mesh.getVerticesBuffer().put(1f);
      mesh.getVerticesBuffer().put(1f);

      mesh.getIndicesBuffer().put((short) (i * 4));
      mesh.getIndicesBuffer().put((short) (i * 4 + 1));
      mesh.getIndicesBuffer().put((short) (i * 4 + 2));
      mesh.getIndicesBuffer().put((short) (i * 4 + 2));
      mesh.getIndicesBuffer().put((short) (i * 4 + 3));
      mesh.getIndicesBuffer().put((short) (i * 4 + 1));

    }
    return mesh;
  }

  private static void addRingVertex(int index, float angle, float radius, boolean n) {
    float x = (float) (Math.cos(angle) * radius);
    float z = (float) (Math.sin(angle) * radius);
    float y = 0;
    if (n)
      y -= 0.1f;
    mesh.getVerticesBuffer().put(x);
    mesh.getVerticesBuffer().put(y);
    mesh.getVerticesBuffer().put(z);
    mesh.getVerticesBuffer().put(0);
    if (n)
      mesh.getVerticesBuffer().put(-1);
    else
      mesh.getVerticesBuffer().put(1);
    mesh.getVerticesBuffer().put(0);

  }

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
        float u = 1f - (float) lon / divisions;
        float v = 1f - (float) lat / divisions;
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
