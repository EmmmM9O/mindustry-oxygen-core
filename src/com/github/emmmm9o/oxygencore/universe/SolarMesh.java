package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;
import com.github.emmmm9o.oxygencore.graphics.OShaders;

import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;

/**
 * SolarMesh
 */
public class SolarMesh implements GenericMesh {
  public Mesh mesh;
  public OPlanet planet;
  public Shader shader;
  public Texture texture;

  public SolarMesh(OPlanet planet, int divisions, Texture texture) {
    this.planet = planet;
    this.mesh = OMeshBuilder.buildIcosphere(divisions);
    this.texture = texture;
    this.shader = OShaders.solarShader;
  }

  public void preRender(UniverseParams params) {

  }

  @Override
  public void render(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform) {
    preRender(params);
    shader.bind();
    shader.setUniformMatrix4("u_view", view.val);
    shader.setUniformMatrix4("u_projection", projection.val);
    shader.setUniformMatrix4("u_model", transform.val);
    shader.setUniformf("u_radius", planet.radius / params.zoom);
    shader.setUniformf("u_zoom", params.zoom);
    shader.setUniformf("u_haloIntensity", 0.00005f);
    shader.setUniformf("u_camera_pos", params.camPos);
    texture.bind(0);
    shader.setUniformi("u_texture", 0);
    shader.apply();
    mesh.render(shader, Gl.triangles);
  }

  @Override
  public void renderPoint(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform) {
    var shader = OShaders.icosphere;
    shader.bind();
    shader.setUniformMatrix4("u_view", view.val);
    shader.setUniformMatrix4("u_projection", projection.val);
    shader.setUniformMatrix4("u_model", transform.val);
    shader.setUniformf("color", planet.lightColor);
    shader.setUniformf("u_radius", planet.pointSize);
    shader.apply();
    mesh.render(shader, Gl.triangles);
  }

}
