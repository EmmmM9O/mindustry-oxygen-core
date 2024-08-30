
package com.github.emmmm9o.oxygencore.universe;

import arc.math.geom.Mat3D;

/**
 * GenericMesh
 */
public interface GenericMesh {
  void render(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform);

  void renderPoint(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform);

}
