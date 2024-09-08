package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.graphics.OMeshBuilder;
import com.github.emmmm9o.oxygencore.graphics.OShaders;

import arc.Core;
import arc.graphics.GL20;
import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.graphics.Texture;
import arc.graphics.gl.Shader;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.MeshBuilder;

/**
 * SolarMesh
 */
public class AtmPlanetMesh implements GenericMesh {
  public Mesh mesh, ringMesh, lineMesh;
  public OPlanet planet;
  public Shader shader, shader2;
  public Texture texture, normal, cloud, ring;

  public AtmPlanetMesh(OPlanet planet, int divisions, Texture texture, Texture normal, Texture cloud) {
    this.planet = planet;
    this.mesh = OMeshBuilder.buildIcosphere(divisions);
    this.texture = texture;
    this.normal = normal;
    this.cloud = cloud;
    this.shader = OShaders.atmPlanetMesh;
    if(planet.hex)this.lineMesh = MeshBuilder.buildHex(Pal.accent,planet.hexDiv,true,1);
  }

  public AtmPlanetMesh(OPlanet planet, int divisions, int ri, Texture texture, Texture ring) {
    this.planet = planet;
    this.mesh = OMeshBuilder.buildIcosphere(divisions);
    this.texture = texture;
    this.ring = ring;
    this.shader = OShaders.atmPlanetMesh;
    this.shader2 = OShaders.ring;
    this.ringMesh = OMeshBuilder.ring(ri, planet.innerRadius, planet.outerRadius);
    if(planet.hex)this.lineMesh = MeshBuilder.buildHex(Pal.accent,planet.hexDiv,true,1);
  }

  public void preRender(UniverseParams params) {

  }

  @Override
  public void render(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform) {
    preRender(params);
    Gl.enable(Gl.cullFace);
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
    shader.setUniformf("u_light_power", planet.lightPower);
    shader.setUniformf("u_mix", planet.mix);
    texture.bind(0);
    shader.setUniformi("u_texture", 0);
    if (normal != null) {
      normal.bind(1);
      shader.setUniformi("u_texture_normal", 1);
      shader.setUniformi("u_no_normal", 0);
    } else {
      shader.setUniformi("u_no_normal", 1);
    }
    if (cloud != null) {
      cloud.bind(2);
      shader.setUniformi("u_texture_cloud", 2);
      shader.setUniformi("u_no_cloud", 0);
    } else {
      shader.setUniformi("u_no_cloud", 1);
    }
    shader.apply();
    mesh.render(shader, Gl.triangles);

    Gl.disable(Gl.cullFace);
    if(params.planet==planet&&planet.hex&&params.zoom<=1){
    var lineShader = OShaders.line;
    Core.gl.glLineWidth(10f/params.zoom);
    lineShader.bind();
    lineShader.setUniformf("u_radius", (planet.radius + planet.atmosphereHeight + planet.lineHight) / params.zoom);
    lineShader.setUniformMatrix4("u_view", view.val);
    lineShader.setUniformMatrix4("u_projection", projection.val);
    lineShader.setUniformMatrix4("u_model", transform.val);
    lineShader.setUniformf("u_zoom", params.zoom);
    lineShader.setUniformf("color", Pal.accent.cpy().a(0.4f));
    lineShader.setUniformf("u_camera_pos", params.camPos);
    lineShader.setUniformf("u_light_pos",
        new Vec3(planet.solarSystem.position).sub(params.planet.position).scl(1f / params.zoom));
    lineShader.setUniformf("u_light_color", new Vec3(planet.lightColor.r, planet.lightColor.g, planet.lightColor.b));
    lineShader.setUniformf("u_ambient_color",
        new Vec3(planet.ambientColor.r, planet.ambientColor.g, planet.ambientColor.b));

    lineShader.setUniformf("u_light_power", planet.lightPower);
    lineShader.apply();

    lineMesh.render(lineShader, Gl.lines);
    Core.gl.glLineWidth(1f);
    }

    if (ring != null) {
      Gl.disable(Gl.cullFace);
      shader2.bind();
      shader2.setUniformMatrix4("u_view", view.val);
      shader2.setUniformMatrix4("u_projection", projection.val);
      shader2.setUniformMatrix4("u_model", transform.val);
      shader2.setUniformf("u_zoom", params.zoom);
      shader2.setUniformf("u_camera_pos", params.camPos);
      shader2.setUniformf("u_light_pos",
          new Vec3(planet.solarSystem.position).sub(params.planet.position).scl(1f / params.zoom));
      shader2.setUniformf("u_light_color", new Vec3(planet.lightColor.r, planet.lightColor.g, planet.lightColor.b));
      shader2.setUniformf("u_ambient_color",
          new Vec3(planet.ambientColor.r, planet.ambientColor.g, planet.ambientColor.b));

      shader2.setUniformf("u_light_power", planet.lightPower);
      ring.bind(0);
      shader2.setUniformi("u_texture", 0);
      shader2.apply();
      ringMesh.render(shader2, Gl.triangles);
    }
    Gl.enable(Gl.cullFace);
  }

  @Override
  public void renderPoint(UniverseParams params, Mat3D view, Mat3D projection, Mat3D transform) {
    var shader = OShaders.icosphere;
    shader.bind();
    shader.setUniformMatrix4("u_view", view.val);
    shader.setUniformMatrix4("u_projection", projection.val);
    shader.setUniformMatrix4("u_model", transform.val);
    shader.setUniformf("color", planet.orbit.color);
    shader.setUniformf("u_radius", planet.pointSize);
    shader.apply();
    mesh.render(shader, Gl.triangles);
  }
}
