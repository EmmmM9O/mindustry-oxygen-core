package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;

import arc.graphics.gl.Shader;

/**
 * ShaderSphereMesh
 */
public class ShaderSphereMesh extends PlanetMesh {

  public ShaderSphereMesh(OPlanet planet, Shader shader, int divisions) {
    super(planet, OMeshBuilder.buildIcosphere(divisions), shader);
  }

  @Override
  public void runRender(UniverseParams params) {
    shader.setUniformf("u_radius", planet.radius / params.zoom);
  }
}
