package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;
import com.github.emmmm9o.oxygencore.graphics.OShaders;

import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;

/**
 * SolarMesh
 */
public class AtmPlanetMesh implements GenericMesh {
  public Mesh mesh;
  public OPlanet planet;
  public Shader shader;
  public Texture texture, normal, cloud;

  public AtmPlanetMesh(OPlanet planet, int divisions, Texture texture, Texture normal, Texture cloud) {
    this.planet = planet;
    this.mesh = OMeshBuilder.buildIcosphere(divisions);
    this.texture = texture;
    this.normal = normal;
    this.cloud = cloud;
    this.shader = OShaders.atmPlanetMesh;
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
    shader.setUniformf("u_radius", (planet.radius + planet.atmosphereHeight) / params.zoom);
    shader.setUniformf("u_zoom", params.zoom);
    shader.setUniformf("u_camera_pos", params.camPos);
    shader.setUniformf("u_light_pos",
        new Vec3(planet.solarSystem.position).sub(params.planet.position).scl(1f / params.zoom));
    shader.setUniformf("u_light_color", new Vec3(planet.lightColor.r, planet.lightColor.g, planet.lightColor.b));
    shader.setUniformf("u_ambient_color",
        new Vec3(planet.ambientColor.r, planet.ambientColor.g, planet.ambientColor.b));
    shader.setUniformf("refractionIndex", planet.refractionIndex);
    shader.setUniformf("refractionPower", planet.refractionPower);
    shader.setUniformf("u_light_power", planet.light_power);
    texture.bind(0);
    shader.setUniformi("u_texture", 0);
    normal.bind(1);
    shader.setUniformi("u_texture_normal", 1);
    if(cloud!=null){
    cloud.bind(2);
    shader.setUniformi("u_texture_cloud", 2);
shader.setUniformi("u_no_cloud", 0);
    }else{
shader.setUniformi("u_no_cloud", 1);
    }
    shader.apply();
    mesh.render(shader, Gl.triangles);

  }

}
