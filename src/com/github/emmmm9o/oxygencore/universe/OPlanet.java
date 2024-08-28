package com.github.emmmm9o.oxygencore.universe;

import static mindustry.Vars.headless;

import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;
import com.github.emmmm9o.oxygencore.ctype.OxygenInfoContent;

import arc.func.Prov;
import arc.graphics.Color;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.graphics.Shaders;
import mindustry.mod.Mods.LoadedMod;

/**
 * OPlanet
 */
public class OPlanet extends OxygenInfoContent {
  public float radius, mass;
  public Orbit orbit;
  public @Nullable OPlanet parent;
  public @Nullable OPlanet solarSystem;
  public Seq<OPlanet> children = new Seq<>();
  public Vec3 position = new Vec3(0, 0, 0);
  public float camRadius;
  public float clipRadius = -1f;
  public boolean bloom;
  public Color lightColor = Color.white.cpy();

  public float gravitational_parameter() {
    return mass * OUniverse.gravitational_constant;
  }

  public OPlanet(String name, LoadedMod mod, OPlanet parent, Orbit orbit, float mass, float radius) {
    super(name, mod);
    this.parent = parent;
    this.orbit = orbit;
    this.mass = mass;
    this.radius = radius;
    if (parent != null) {
      parent.children.add(this);
    }
    clipRadius = Math.max(clipRadius, radius + 0.5f);
    for (solarSystem = this; solarSystem.parent != null; solarSystem = solarSystem.parent)
      ;
  }

  public @Nullable GenericMesh mesh;
  public Prov<GenericMesh> meshLoader = () -> new ShaderSphereMesh(this, Shaders.unlit, 2);

  @Override
  public void load() {
    super.load();
    if (!headless) {
      mesh = meshLoader.get();
    }

  }

  public void draw(UniverseParams params, Mat3D projection, Mat3D transform) {
    mesh.render(params, projection, transform);
  }

  @Override
  public OxygenContentType getContentType() {
    return OxygenContentType.oplanet;
  }

  public Mat3D getTransform(Mat3D mat) {
    return mat.setToTranslation(position);
  }
}
