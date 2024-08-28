package com.github.emmmm9o.oxygencore.universe;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Cubemap;
import arc.graphics.Gl;
import arc.graphics.g2d.Bloom;
import arc.graphics.g2d.Draw;
import arc.graphics.g3d.Camera3D;
import arc.graphics.g3d.PlaneBatch3D;
import arc.graphics.g3d.VertexBatch3D;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import mindustry.graphics.CubemapMesh;
import mindustry.graphics.Pal;
import arc.util.Disposable;
import arc.util.Tmp;

/**
 * UniverseRenderer
 */
public class UniverseRenderer implements Disposable {

  public static final float outlineRad = 10f, camLength = 4f;
  public static final Color outlineColor = Pal.accent.cpy().a(1f),
      hoverColor = Pal.accent.cpy().a(0.5f),
      borderColor = Pal.accent.cpy().a(0.3f),
      shadowColor = new Color(0, 0, 0, 0.7f);
  public final Camera3D cam = new Camera3D();
  public final VertexBatch3D batch = new VertexBatch3D(20000, false, true, 0);
  public final CubemapMesh skybox = new CubemapMesh(new Cubemap("cubemaps/stars/"));
  public final Bloom bloom = new Bloom(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, true, false) {
    {
      setThreshold(0.8f);
      blurPasses = 6;
    }
  };
  public final Mat3D mat = new Mat3D();
  public final PlaneBatch3D projector = new PlaneBatch3D();

  @Override
  public void dispose() {
    projector.dispose();
    skybox.dispose();
    batch.dispose();
    bloom.dispose();
  }

  public UniverseRenderer() {
    projector.setScaling(1f / 150f);
    cam.fov = 60f;
    cam.far = 150f;
  }

  public void render(UniverseParams params) {
    Draw.flush();
    Gl.clear(Gl.depthBufferBit);
    Gl.enable(Gl.depthTest);
    Gl.depthMask(true);

    Gl.enable(Gl.cullFace);
    Gl.cullFace(Gl.back);
    int w = Core.graphics.getWidth();
    int h = Core.graphics.getHeight();
    bloom.blending = false;
    cam.up.set(Vec3.Y);

    cam.resize(w, h);
    if (params.zoom <= 100) {
      params.camPos.setLength((params.zoom) * params.planet.radius + 50f);
      cam.far = params.zoom * Math.max(params.planet.radius, 400f) + 400f;

      cam.position.set(params.planet.position).add(params.camPos);
      cam.lookAt(params.planet.position);
    } else {
      cam.far = Math.max(400f, params.planet.radius) + 10000f;
      params.camPos.setLength(200f);
      var pos = Tmp.v34.set(params.planet.position).scl(1f/params.zoom);
      cam.position.set(pos).add(params.camPos);
      cam.lookAt(pos);
    }
    cam.update();
    params.camUp.set(cam.up);
    params.camDir.set(cam.direction);

    projector.proj(cam.combined);
    batch.proj(cam.combined);

    bloom.resize(w, h);
    bloom.capture();

    Vec3 lastPos = Tmp.v31.set(cam.position);
    cam.position.setZero();
    cam.update();

    Gl.depthMask(false);

    skybox.render(cam.combined);

    Gl.depthMask(true);

    cam.position.set(lastPos);
    cam.update();

    OPlanet solarSystem = params.planet.solarSystem;
    renderPlanet(solarSystem, params);
    renderTransparent(solarSystem, params);
    bloom.render();
    Gl.enable(Gl.blend);
    Gl.disable(Gl.cullFace);
    Gl.disable(Gl.depthTest);

    cam.update();
  }

  public void renderPlanet(OPlanet planet, UniverseParams params) {
    cam.update();
    if (params.zoom <= 100) {
      if (cam.frustum.containsSphere(planet.position, planet.clipRadius)) {
        // render planet at offsetted position in the world
        planet.draw(params, cam.combined, planet.getTransform(mat));
      }
    }

    for (OPlanet child : planet.children) {
      renderPlanet(child, params);
    }

  }

  public void renderTransparent(OPlanet planet, UniverseParams params) {
    for (OPlanet child : planet.children) {
      renderTransparent(child, params);
    }

    batch.proj(cam.combined);
    renderOrbit(planet, params);
  }

  public void renderOrbit(OPlanet planet, UniverseParams params) {
    if (planet.parent == null)
      return;
    if (params.zoom <= 100) {
      Vec3 center = planet.parent.position;
      for (var point : planet.orbit.points) {
        batch.vertex(Tmp.v32.set(center).add(point), planet.orbit.color);
      }
    } else {
      var t = planet.parent.position;
      Vec3 center = new Vec3(t.x / params.zoom, t.y / params.zoom, t.z / params.zoom);
      for (var point : planet.orbit.points) {
        batch.vertex(Tmp.v32.set(center).add(point.x / params.zoom, point.y / params.zoom, point.z / params.zoom),
            planet.orbit.color);
      }
    }
    batch.flush(Gl.lineLoop);
  }
}
