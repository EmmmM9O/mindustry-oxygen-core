/* (C) 2025 */
package oxygen.ui;

import mindustry.graphics.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;

import static arc.Core.*;
import static oxygen.ui.OCPal.*;

/**
 * OCLoadRenderer
 */
public class OCLoadRenderer implements LoadRendererI {
  @Override
  public void dispose() {

  }

  @Override
  public void draw() {
    graphics.clear(odarkl1);
    float w = graphics.getWidth(), h = graphics.getHeight(), s = Scl.scl();
    Draw.proj().setOrtho(0, 0, graphics.getWidth(), graphics.getHeight());
    int lightVerts = 20;
    float lightRad = Math.max(w, h) * 0.6f;
    float stroke = 5f * s;
    Fill.light(w / 2, h / 2, lightVerts, lightRad, Tmp.c1.set(ogreen).a(0.2f), Color.clear);
    Draw.flush();
  }
}
