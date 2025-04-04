/* (C) 2025 */
package oxygen.type;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.*;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import static oxygen.core.OCVars.*;

public class OUnitType extends UnitType {
  public float maxHeight = 10f;

  public OUnitType(String name) {
    super(name);
  }

  public float drawX, drawY, drawScl;

  @Override
  public void draw(Unit unit) {
    if (unit.inFogTo(Vars.player.team()))
      return;
    boolean isPayload = !unit.isAdded();
    float z = isPayload ? Draw.z()
        : unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit)
            : groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);


    float height = unit.elevation * maxHeight;
    if (height >= renderer.cameraHeight) {
      if (!isPayload && (unit.isFlying() || shadowElevation > 0)) {
        Draw.z(Math.min(Layer.darkness, z - 1f));
        drawShadow(unit);
      }
      drawLight(unit);
      Draw.reset();
      return;
    }
    drawX =
        (unit.x - Core.camera.position.x) / (renderer.cameraHeight - height) * renderer.cameraHeight
            + Core.camera.position.x;
    drawY =
        (unit.y - Core.camera.position.y) / (renderer.cameraHeight - height) * renderer.cameraHeight
            + Core.camera.position.y;
    drawScl = renderer.cameraHeight / (renderer.cameraHeight - height) / 4f;

    Mechc mech = unit instanceof Mechc ? (Mechc) unit : null;
    if (unit.controller().isBeingControlled(Vars.player.unit())) {
      drawControl(unit);
    }
    if (!isPayload && (unit.isFlying() || shadowElevation > 0)) {
      Draw.z(Math.min(Layer.darkness, z - 1f));
      drawShadow(unit);
    }
    Draw.z(z - 0.02f);

    if (mech != null) {

    }
    Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

    if (unit instanceof Payloadc) {
      drawPayload((Unit & Payloadc) unit);
    }

    // drawSoftShadow(unit);

    Draw.z(z);
    if (drawBody)
      drawOutline(unit);
    drawWeaponOutlines(unit);
    if (engineLayer > 0)
      Draw.z(engineLayer);
    if (trailLength > 0 && !naval && (unit.isFlying() || !useEngineElevation)) {
      drawTrail(unit);
    }
    /*
     * if (engines.size > 0) drawEngines(unit);
     */
    Draw.z(z);
    if (drawBody)
      drawBody(unit);
    if (drawCell)
      drawCell(unit);
    drawWeapons(unit);
    if (drawItems)
      drawItems(unit);
    drawLight(unit);

    if (unit.shieldAlpha > 0 && drawShields) {
      drawShield(unit);
    }
    if (parts.size > 0) {
      for (int i = 0; i < parts.size; i++) {
        var part = parts.get(i);

        WeaponMount first =
            unit.mounts.length > part.weaponIndex ? unit.mounts[part.weaponIndex] : null;
        if (first != null) {
          DrawPart.params.set(first.warmup, first.reload / weapons.first().reload,
              first.smoothReload, first.heat, first.recoil, first.charge, drawX, drawY,
              unit.rotation);
        } else {
          DrawPart.params.set(0f, 0f, 0f, 0f, 0f, 0f, drawX, drawY, unit.rotation);
        }

        if (unit instanceof Scaled s) {
          DrawPart.params.life = s.fin();
        }

        part.draw(DrawPart.params);
      }
    }

    if (!isPayload) {
      for (Ability a : unit.abilities) {
        Draw.reset();
        a.draw(unit);
      }
    }
    Draw.reset();
  }

  @Override
  public void drawCell(Unit unit) {
    applyColor(unit);

    Draw.color(cellColor(unit));
    Draw.rect(cellRegion, drawX, drawY, cellRegion.width * drawScl, cellRegion.height * drawScl,
        unit.rotation - 90);
    Draw.reset();
  }

  @Override
  public void drawBody(Unit unit) {
    applyColor(unit);
    Draw.rect(region, drawX, drawY, region.width * drawScl, region.height * drawScl,
        unit.rotation - 90);

    Draw.reset();
  }

  @Override
  public void drawLight(Unit unit) {
    if (lightRadius > 0) {
      Drawf.light(drawX, drawY, lightRadius, lightColor, lightOpacity);
    }
  }

  @Override
  public void drawShadow(Unit unit) {
    float lightE = unit.elevation * maxHeight / renderer.lightDir.z;
    Draw.color(Pal.shadow, Pal.shadow.a);
    Draw.rect(shadowRegion, unit.x + renderer.lightDir.x * lightE,
        unit.y + renderer.lightDir.y * lightE, unit.rotation - 90);
    Draw.color();
  }
}
