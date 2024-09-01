package com.github.emmmm9o.oxygencore.universe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.github.emmmm9o.oxygencore.core.Manager;
import com.github.emmmm9o.oxygencore.ctype.OxygenContentType;

import arc.Core;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.serialization.Base64Coder;

/**
 * Universe
 * length AT 1e6 m
 * mass MT 1e24 kg
 */
public class OUniverse {
  public int seconds = 0;
  public int dyear = 0;// 50yaers
  public float secondCounter;
  public int timeScl = 1;

  public static final float gravitational_constant = 6.67430e-5f;
  public int lastSecond1 = 0, lastSecond2 = 0;

  public OUniverse() {
    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    load();
    loadPl();
    updateGlobal();
  }

  public void updatePlanet(OPlanet planet) {
    if (planet.orbit == null) {
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

  public void update() {
    secondCounter += Time.delta / 60f * timeScl;
    if (secondCounter >= 1) {
      seconds += (int) secondCounter;
      secondCounter %= 1f;
      int k1 = seconds / 60 * 60;
      int k2 = seconds / 60;
      if (k1 > lastSecond1) {
        lastSecond1 = k1;
        // update planet pos every hours
        updateGlobal();
      }
      if (k2 > lastSecond2) {
        lastSecond2 = k2;
        save();
      }
      if (seconds >= 365 * 24 * 60 * 60 * 50) {
        lastSecond1 = 0;
        lastSecond2 = 0;
        dyear += 1;
        seconds = 0;
        setAllStart();
        savePl();
      }
    }
  }

  public void save() {
    Core.settings.put("universetime", seconds);
  }

  public void load() {
    seconds = Core.settings.getInt("universetime", 0);
    dyear = Core.settings.getInt("universeyear", 0);
  }

  public void setAllStart() {
    for (var planet : Manager.content.oplanets()) {
      planet.setStart();
    }
  }

  public void savePl() {
    Core.settings.put("universeyear", dyear);
    var planets = Manager.content.oplanets();

    var baos = new ByteArrayOutputStream();
    var writer = new Writes(new DataOutputStream(baos));


    writer.i(planets.size);
    for (var planet : planets) {
      writer.str(planet.name);
      planet.write(writer);
    }
    writer.close();
    Core.settings.put("universedata", new String(Base64Coder.encode(baos.toByteArray())));
  }

  public void loadPl() {
    var data = Core.settings.getString("universedata", "ERROR");
    if (data == "ERROR") {
      return;
    }
    var reader = new Reads(new DataInputStream(new ByteArrayInputStream(Base64Coder.decode(data))));
    var size = reader.i();
    while ((size--) > 0) {
      var name = reader.str();
      var planet = Manager.content.getByName(OxygenContentType.oplanet, name);
      if (planet == null) {
        Log.err("ERROR loading planets: no planet @", name);
        return;
      }
      ((OPlanet) planet).read(reader);
    }
    reader.close();
  }

  public void resetTime() {
    seconds = 0;
    dyear = 0;
    lastSecond1 = 0;
    lastSecond2 = 0;
    save();
    for (var planet : Manager.content.oplanets()) {
      planet.resetAll();
    }
    savePl();
    updateGlobal();
  }

  public DateTimeFormatter formatter;

  public String getTimeString() {
    return LocalDateTime
        .ofInstant(Instant.ofEpochSecond(seconds + dyear * 365L * 24L * 60L * 60L * 50L), ZoneId.systemDefault())
        .format(formatter);

  }
  // AT3*MT-1s-2
}
