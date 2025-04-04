/* (C) 2025 */
package oxygen.graphics;

import arc.*;
import arc.math.geom.*;
import mindustry.*;

public class OCRenderer implements ApplicationListener {
  // scl 4 height 100
  public float cameraHeight = 50f, cameraNear = 1f, sclT = 4 * cameraHeight;
  public Vec3 lightDir = new Vec3(-0.2f, -0.2f, 0.5f).nor();

  public OCRenderer() {}

  @Override
  public void update() {
    cameraHeight = sclT / Vars.renderer.getDisplayScale();
  }
}
