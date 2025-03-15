/* (C) 2025 */
package oxygen.type;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import static oxygen.core.OCVars.*;

public class OUnitType extends UnitType {
  public float maxHeight = 10f;

  public OUnitType(String name) {
    super(name);
  }

  @Override
  public void drawShadow(Unit unit) {
    float e = unit.elevation * maxHeight / renderer.lightDir.y;
    Draw.color(Pal.shadow, Pal.shadow.a);
    Draw.rect(shadowRegion, unit.x + renderer.lightDir.x * e, unit.y + renderer.lightDir.y * e,
        unit.rotation - 90);
    Draw.color();
  }
}
