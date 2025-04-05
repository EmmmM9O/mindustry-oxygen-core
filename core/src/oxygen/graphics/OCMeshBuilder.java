/* (C) 2025 */
package oxygen.graphics;

import arc.graphics.*;

public class OCMeshBuilder {
  private static Mesh tmp;

  public static Mesh screenMesh() {
    tmp = new Mesh(true, 4, 6, VertexAttribute.position);
    tmp.setVertices(new float[] { //
        -1f, -1f, //
        -1f, 1f, //
        1f, 1f, //
        1f, -1f,//
    });
    tmp.setIndices(new short[] {0, 1, 2, 0, 2, 3});
    return end();
  }

  public static Mesh end() {
    Mesh last = tmp;
    tmp = null;
    return last;
  }
}
