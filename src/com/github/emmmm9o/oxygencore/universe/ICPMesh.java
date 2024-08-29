package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.Texture;
import arc.graphics.gl.Shader;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;

/**
 * ICPMesh
 */
public class ICPMesh extends PlanetMesh {
  public Texture texture;
  public int divisions;

  public ICPMesh(OPlanet planet, Shader shader, int divisions, Texture texture) {
    super(planet, OMeshBuilder.buildIcosphere(divisions), shader);
    this.divisions = divisions;
    this.texture = texture;
    this.shader = shader;
  }

  @Override
  public void runRender(UniverseParams params) {
    shader.setUniformf("u_radius", planet.radius / params.zoom);
    texture.bind(0);
    shader.setUniformi("u_texture", 0);
  }

}
