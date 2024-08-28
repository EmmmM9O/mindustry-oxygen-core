package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.core.Manager;

/**
 * Universe
 * length AT 1e6 m
 * mass MT 1e24 kg
 */
public class OUniverse {
  public int seconds = 0;
  public static final float gravitational_constant = 6.67430e-5f;

  public void updatePlanet(OPlanet planet) {
	  if(planet.orbit==null) {
for (OPlanet child : planet.children) {
      updatePlanet(child);
    }
return;
	  }
    planet.position.setZero();
    planet.position.add(planet.orbit.calculatePositionT(seconds));
    if (planet.parent != null) {
      planet.position.add(planet.parent.position);
    }
    for (OPlanet child : planet.children) {
      updatePlanet(child);
    }
  }

  public void updateGlobal() {
    for (var planet : Manager.content.oplanets()) {
      if (planet.parent == null)
        updatePlanet(planet);
    }
  }
  // AT3*MT-1s-2
}
