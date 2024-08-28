package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.gl.Shader;
import mindustry.graphics.g3d.MeshBuilder;

/**
 * ShaderSphereMesh
 */
public class ShaderSphereMesh extends PlanetMesh {
  public ShaderSphereMesh(OPlanet planet, Shader shader, int divisions) {
    super(planet, MeshBuilder.buildIcosphere(divisions, planet.radius), shader);
  }
}
