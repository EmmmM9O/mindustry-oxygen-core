package com.github.emmmm9o.oxygencore.universe;

import arc.math.geom.Vec3;

/**
 * UniverseParams
 */
public class UniverseParams {

  public Vec3 camUp = new Vec3(0f, 1f, 0f);
  /** the unit length direction vector of the camera **/
  public Vec3 camDir = new Vec3(0, 0, -1);
  public Vec3 camPos = new Vec3(0f, 0f, 4f);
  public OPlanet planet = OPlanets.Sun;
  public float zoom = 1f;

}
