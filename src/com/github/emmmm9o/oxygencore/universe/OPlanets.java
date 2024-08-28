package com.github.emmmm9o.oxygencore.universe;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.graphics.Color;
import arc.math.Mathf;

/**
 * OPlanets
 */
public class OPlanets {

  public static OPlanet Sol, Earth, Halley, Moon, Mercury, Venus, Mars,
      Jupiter, Saturn, Uranus, Neptune;

  public static void load() {
    Sol = new OPlanet("Sol", Manager.mod, null, null,
        1.9891e6f, 696f);
    Sol.bloom = true;
    Mercury = new OPlanet("Mercury", Manager.mod, Sol,
        new Orbit(57600f, 0.205630f, 7.00487f
            * Mathf.degreesToRadians,
            29.124f * Mathf.degreesToRadians,
            48.331f * Mathf.degreesToRadians,
            4.402608f,
            Sol),
        3.3011e-1f, 2.44f);
    Mercury.orbit.color = Color.sky;
    Venus = new OPlanet("Venus", Manager.mod, Sol,
        new Orbit(0.723332f * 1.496e5f, 0.006772f, 3.39458f
            * Mathf.degreesToRadians,
            50.115f * Mathf.degreesToRadians,
            76.680f * Mathf.degreesToRadians,
            3.176145f,
            Sol),
        4.8675f,
        6.0536f);
    Venus.orbit.color = Color.gold;
    Earth = new OPlanet("Earth", Manager.mod, Sol,
        new Orbit(149598.023f, 0.0167086f, 7.155f
            * Mathf.degreesToRadians,
            114.20783f * Mathf.degreesToRadians,
            -11.26064f * Mathf.degreesToRadians,
            1.753434f, Sol),
        5.97237f, 6.371f);
    Earth.orbit.color = Color.acid;
    Moon = new OPlanet("Moon", Manager.mod, Earth,
        new Orbit(384.403f, 0.0549f, 18.28f
            * Mathf.degreesToRadians,
            318.15f * Mathf.degreesToRadians,
            125.08f * Mathf.degreesToRadians,
            0f,
            Earth),
        7.342e-2f, 1.87628f);
    Moon.orbit.color = Color.white;
    Mars = new OPlanet("Mars", Manager.mod, Sol,
        new Orbit(1.523679f * 1.496e5f, 0.09341233f, 1.850f
            * Mathf.degreesToRadians,
            5.865019f - 0.8653088f,
            49.558f * Mathf.degreesToRadians,
            6.203831f, Sol),
        6.4171e-1f, 3.3895f);
    Mars.orbit.color = Color.red;
    Jupiter = new OPlanet("Jupiter", Manager.mod, Sol, new Orbit(7.784120267e4f,
        0.04839266f, 0.0227818f,
        0.257503f - 1.755036f, 1.755036f, 0.600470f, Sol),
        1.8986111e3f, 69.9110f);
    Jupiter.orbit.color = Color.brown;
    Saturn = new OPlanet("Saturn", Manager.mod, Sol, new Orbit(
        1.426725413e6f, 0.05415060f, 0.0433620f,
        1.613242f - 1.984702f, 1.984702f, 0.871693f, Sol),
        5.6846272e2f, 58.2320f);
    Saturn.orbit.color = Color.orange;
    Uranus = new OPlanet("Uranus", Manager.mod, Sol, new Orbit(
        2.87097222e6f, 0.04716771f, 0.01343659f,
        2.983889f - 1.295558f, 1.295558f, 5.4669329f,
        Sol), 8.6832054e1f, 25.3620f);
    Uranus.orbit.color = Color.green;
    Neptune = new OPlanet("Neptune", Manager.mod, Sol,
        new Orbit(4.49825291e6f, 0.00858587f, 0.0308778f,
            0.784898f - 2.298977f,
            2.298977f,
            5.321160f, Sol),
        1.024569e2f, 24.6240f);
    Neptune.orbit.color = Color.blue;
    Halley = new OPlanet("Halley", Manager.mod, Sol,
        new Orbit(17.8f * 1.496e5f, 0.967f, 162.3f
            * Mathf.degreesToRadians,
            112.26f * Mathf.degreesToRadians,
            59.11f * Mathf.degreesToRadians,
            4.61873554527679f, Sol),
        1e-9f, 5f);

    Halley.orbit.color = Color.purple;

  }
}
