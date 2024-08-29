package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;

/**
 * PlanetMesh
 */
public abstract class PlanetMesh implements GenericMesh {
  protected Mesh mesh;
  protected OPlanet planet;
  protected Shader shader;

  public PlanetMesh(OPlanet planet, Mesh mesh, Shader shader) {
    this.planet = planet;
    this.mesh = mesh;
    this.shader = shader;
  }

  public PlanetMesh() {
  }

  /**
   * Should be overridden to set up any shader parameters such as planet position,
   * normals, etc.
   */
  public void preRender(UniverseParams params) {

  }

  public void runRender(UniverseParams params) {

  }

  @Override
  public void render(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform) {
    preRender(params);
    shader.bind();
    shader.setUniformMatrix4("u_view", view.val);
    shader.setUniformMatrix4("u_projection", projection.val);
    shader.setUniformMatrix4("u_model", transform.val);
    runRender(params);
    shader.apply();
    mesh.render(shader, Gl.triangles);
  }

}