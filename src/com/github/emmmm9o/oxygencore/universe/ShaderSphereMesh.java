package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.gl.Shader;
import mindustry.graphics.g3d.MeshBuilder;

/**
 * ShaderSphereMesh
 */
public class ShaderSphereMesh extends PlanetMesh {
  public int divisions;
  public float last_zoom;

  public ShaderSphereMesh(OPlanet planet, Shader shader, int divisions) {
    super(planet, MeshBuilder.buildIcosphere(divisions, planet.radius), shader);
    this.divisions = divisions;
    this.last_zoom = 1f;
  }

  @Override
  public void preRender(UniverseParams params) {
    if (Math.abs(last_zoom - params.zoom) > 0.1) {
      last_zoom = params.zoom;
      mesh = MeshBuilder.buildIcosphere(divisions, planet.radius / params.zoom);
    }
  }
}
