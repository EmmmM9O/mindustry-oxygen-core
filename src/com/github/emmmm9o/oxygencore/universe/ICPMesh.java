package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.Gl;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;

/**
 * ICPMesh
 */
public class ICPMesh extends PlanetMesh {
  public Texture texture;
  public int divisions;

  public ICPMesh(OPlanet planet, Shader shader, int divisions, Texture texture) {
    super(planet, OMeshBuilder.buildIcosphere(divisions, planet.radius), shader);
    this.divisions = divisions;
    this.texture = texture;
    this.shader = shader;
  }

  @Override
  public void preRender(UniverseParams params) {

  }

  @Override
  public void render(UniverseParams params, Mat3D projection, Mat3D transform) {
    preRender(params);
    shader.bind();
    shader.setUniformMatrix4("u_proj", projection.val);
    shader.setUniformMatrix4("u_trans", transform.val);
    shader.setUniformf("u_radius", planet.radius / params.zoom);
    texture.bind(0);
    shader.setUniformi("u_texture", 0);
    shader.apply();
    mesh.render(shader, Gl.triangles);
  }
}
