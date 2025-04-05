/* (C) 2025 */
package oxygen.graphics;

import arc.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.game.EventType.*;
import oxygen.graphics.menu.*;
import oxygen.graphics.universe.*;

public class OCRenderer implements ApplicationListener {
  // scl 4 height 100
  public float cameraHeight = 50f, cameraNear = 1f, sclT = 4 * cameraHeight;
  public Vec3 lightDir = new Vec3(-0.2f, -0.2f, 0.5f).nor();
  public UniverseRenderer universeRenderer;
  public OCMenuRendererI menuRenderer;

  public OCRenderer() {}

  @Override
  public void init() {
    universeRenderer = new UniverseRenderer();
    Events.on(ResizeEvent.class, e -> {
      universeRenderer.resize();
    });
  }

  public OCMenuRendererI getMenuRenderer() {
    return menuRenderer = new MenuBlackhole();
  }

  @Override
  public void update() {
    cameraHeight = sclT / Vars.renderer.getDisplayScale();
  }

  @Override
  public void dispose() {}
}
