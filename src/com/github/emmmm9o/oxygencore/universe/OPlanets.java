package com.github.emmmm9o.oxygencore.universe;

import javax.management.openmbean.ArrayType;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.graphics.OShaders;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.math.Mathf;

/**
 * OPlanets
 */
public class OPlanets {

                public static OPlanet Sun, Earth, Halley, Moon, Mercury, Venus, Mars,
                                                Jupiter, Saturn, Uranus, Neptune,Io;

                public static void load() {
                                var path = Manager.mod.root.child("sprites").child("planets");
                                Sun = new OPlanet("Sun", Manager.mod, null, null,
                                                                1.9891e6f, 696f);
                                Sun.bloom = true;
                                Sun.meshLoader = () -> new SolarMesh(Sun, 64,
                                                                new Texture(path.child("sun.png")));
                                Sun.dayPeriod = 25.05f * 24 * 60 * 60;
                                Sun.axialTilt = 0;
                                Mercury = new OPlanet("Mercury", Manager.mod, Sun,
                                                                new Orbit(57600f, 0.205630f, 7.00487f
                                                                                                * Mathf.degreesToRadians,
                                                                                                29.124f * Mathf.degreesToRadians,
                                                                                                48.331f * Mathf.degreesToRadians,
                                                                                                4.402608f,
                                                                                                Sun),
                                                                3.3011e-1f, 2.44f);
                                Mercury.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Mercury.dayPeriod = 58.65f * 24 * 60 * 60;
                                Mercury.axialTilt = 0.034f;
                                Mercury.meshLoader = () -> new AtmPlanetMesh(Mercury, 16,
                                                                new Texture(path.child("mercury.png")),
                                                                new Texture(path.child("mercury-normal.png")),
                                                                null);
                                Mercury.orbit.color = Color.sky;
				Mercury.hexDiv=2;
				Mercury.lineHight=0.05f;
                                Venus = new OPlanet("Venus", Manager.mod, Sun,
                                                                new Orbit(0.723332f * 1.496e5f, 0.006772f, 3.39458f
                                                                                                * Mathf.degreesToRadians,
                                                                                                50.115f * Mathf.degreesToRadians,
                                                                                                76.680f * Mathf.degreesToRadians,
                                                                                                3.176145f,
                                                                                                Sun),
                                                                4.8675f,
                                                                6.0536f);
                                Venus.dayPeriod = 243 * 24 * 60 * 60;
                                Venus.axialTilt = 2.64f;
                                Venus.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Venus.meshLoader = () -> new AtmPlanetMesh(Venus, 32,
                                                                new Texture(path.child("venus.png")),
                                                                new Texture(path.child("venus-normal.png")),
                                                                new Texture(path.child("venus-cloud.png")));
                                Venus.orbit.color = Color.gold;
                                Venus.mix = 0.9f;
                                Venus.atmosphereHeight = 0.14f * 92f;
                                Venus.atmosphere = true;
                                Earth = new OPlanet("Earth", Manager.mod, Sun,
                                                                new Orbit(149598.023f, 0.0167086f, 7.155f
                                                                                                * Mathf.degreesToRadians,
                                                                                                114.20783f * Mathf.degreesToRadians,
                                                                                                -11.26064f * Mathf.degreesToRadians,
                                                                                                1.753434f, Sun),
                                                                5.97237f, 6.371f);
                                Earth.atmosphere = true;
                                Earth.lightColor = new Color(0.65f, 0.58f, 0.5f, 1.0f);
                                Earth.meshLoader = () -> new AtmPlanetMesh(Earth, 32,
                                                                new Texture(path.child("earth.png")),
                                                                new Texture(path.child("earth-normal.png")),
                                                                new Texture(path.child("earth-cloud.png")));
                                Earth.orbit.color = Color.acid;
                                Earth.lightPower = 0.003f;
                                Earth.atmosphere = true;
                                Earth.mix = 0.3f;
                                Moon = new OPlanet("Moon", Manager.mod, Earth,
                                                                new Orbit(384.403f, 0.0549f, 18.28f
                                                                                                * Mathf.degreesToRadians,
                                                                                                318.15f * Mathf.degreesToRadians,
                                                                                                125.08f * Mathf.degreesToRadians,
                                                                                                0f,
                                                                                                Earth),
                                                                7.342e-2f, 1.87628f);
                                Moon.orbit.color = Color.white;
                                Moon.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Moon.meshLoader = () -> new AtmPlanetMesh(Moon, 16,
                                                                new Texture(path.child("moon.png")),
                                                                new Texture(path.child("moon-normal.png")),
                                                                null);
                                Moon.dayPeriod = 27.32f * 60 * 60 * 24;
                                Moon.axialTilt = 0;
				Moon.hexDiv=2;
				Moon.lineHight=0.05f;
                                Mars = new OPlanet("Mars", Manager.mod, Sun,
                                                                new Orbit(1.523679f * 1.496e5f, 0.09341233f, 1.850f
                                                                                                * Mathf.degreesToRadians,
                                                                                                5.865019f - 0.8653088f,
                                                                                                49.558f * Mathf.degreesToRadians,
                                                                                                6.203831f, Sun),
                                                                6.4171e-1f, 3.3895f);
                                Mars.orbit.color = Color.red;
                                Mars.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Mars.atmosphereHeight = 0.128f;
                                Mars.atmosphere = true;
                                Mars.meshLoader = () -> new AtmPlanetMesh(Mars, 32,
                                                                new Texture(path.child("mars.png")),
                                                                new Texture(path.child("mars-normal.png")),
                                                                new Texture(path.child("mars-cloud.png")));
                                Mars.mix = 0.05f;
                                Mars.dayPeriod = 24 * 60 * 60;
                                Mars.axialTilt = 25.19f;
				Mars.lineHight=0.08f;
                                Jupiter = new OPlanet("Jupiter", Manager.mod, Sun, new Orbit(7.784120267e4f,
                                                                0.04839266f, 0.0227818f,
                                                                0.257503f - 1.755036f, 1.755036f, 0.600470f, Sun),
                                                                1.8986111e3f, 69.9110f);
                                Jupiter.orbit.color = Color.brown;
                                Jupiter.lightColor = new Color(0.65f, 0.65f, 0.65f, 1.0f);
				Jupiter.ambientColor = new Color(0.2f,0.2f,0.2f,1f);
                                Jupiter.meshLoader = () -> new AtmPlanetMesh(Jupiter, 48,
                                                                new Texture(path.child("jupiter.png")), null, null);
                                Jupiter.atmosphere = true;
                                Jupiter.gas = true;
                                Jupiter.dayPeriod = 10 * 60 * 60;
                                Jupiter.axialTilt = 3.13f;
                                Jupiter.lightPower = 0.002f;
				Jupiter.hex=false;
                                Io = new OPlanet("Io", Manager.mod, Jupiter,
                                                                new Orbit(4.218e2f, 0.0041f, 0.0006458f,
                                                                                                1.7058f-5.46255f,
                                                                                                5.46255f,
                                                                                                1.8627f,
                                                                                                Jupiter),
                                                                8.93e-2f,1.821f);
                                Io.orbit.color = Color.gold;
                                Io.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
				Io.lineHight=0.05f;
				Io.hexDiv=2;
                                Io.meshLoader = () -> new AtmPlanetMesh(Io, 32,
                                                                new Texture(path.child("io.png")),
                                                                new Texture(path.child("io-normal.png")),
                                                                null);
                                Saturn = new OPlanet("Saturn", Manager.mod, Sun, new Orbit(
                                                                1.426725413e6f, 0.05415060f, 0.0433620f,
                                                                1.613242f - 1.984702f, 1.984702f, 0.871693f, Sun),
                                                                5.6846272e2f, 58.2320f);
                                Saturn.orbit.color = Color.orange;
                                Saturn.atmosphere = true;
                                Saturn.gas = true;
                                Saturn.ring = true;
                                Saturn.outerRadius = 200f;
                                Saturn.innerRadius = 66f;
                                Saturn.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
				Saturn.hex=false;
                                Saturn.meshLoader = () -> new AtmPlanetMesh(Saturn, 48,
                                                                64, new Texture(path.child("saturn.png")),
                                                                new Texture(path.child("saturn-ring.png")));
                                Saturn.axialTilt = 26.73f;
                                Saturn.dayPeriod = 10 * 60 * 60;
                                Uranus = new OPlanet("Uranus", Manager.mod, Sun, new Orbit(
                                                                2.87097222e6f, 0.04716771f, 0.01343659f,
                                                                2.983889f - 1.295558f, 1.295558f, 5.4669329f,
                                                                Sun), 8.6832054e1f, 25.3620f);
                                Uranus.orbit.color = Color.green;
                                Uranus.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Uranus.meshLoader = () -> new AtmPlanetMesh(Uranus, 48,
                                                                new Texture(path.child("uranus.png")), null, null);
                                Uranus.axialTilt = 97.77f;
                                Uranus.dayPeriod = 17 * 60 * 60;
				Uranus.gas = true;
				Uranus.hex=false;
                                Neptune = new OPlanet("Neptune", Manager.mod, Sun,
                                                                new Orbit(4.49825291e6f, 0.00858587f, 0.0308778f,
                                                                                                0.784898f - 2.298977f,
                                                                                                2.298977f,
                                                                                                5.321160f, Sun),
                                                                1.024569e2f, 24.6240f);
                                Neptune.orbit.color = Color.blue;
                                Neptune.lightColor = new Color(0.5f, 0.5f, 0.5f, 1.0f);
                                Neptune.meshLoader = () -> new AtmPlanetMesh(Neptune, 48,
                                                                new Texture(path.child("neptune.png")), null, null);
                                Neptune.dayPeriod = 16 * 60 * 60;
                                Neptune.axialTilt = 28.32f;
				Neptune.gas = true;
				Neptune.hex=false;
                                Halley = new OPlanet("Halley", Manager.mod, Sun,
                                                                new Orbit(17.8f * 1.496e5f, 0.967f, 162.3f
                                                                                                * Mathf.degreesToRadians,
                                                                                                112.26f * Mathf.degreesToRadians,
                                                                                                59.11f * Mathf.degreesToRadians,
                                                                                                4.61873554527679f, Sun),
                                                                1e-9f, 5f);

                                Halley.orbit.color = Color.purple;

                }
}
