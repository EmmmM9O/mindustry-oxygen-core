/* (C) 2025 */
package oxygen.ui;

import mindustry.graphics.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;

import static arc.Core.*;
import static oxygen.ui.OCPal.*;


public class OCLoadRenderer implements LoadRendererI {
  @Override
  public void dispose() {

  }

  @Override
  public void draw() {
    graphics.clear(odarkl1);
    float w = graphics.getWidth(), h = graphics.getHeight();
    Draw.proj().setOrtho(0, 0, graphics.getWidth(), graphics.getHeight());
    int lightVerts = 20;
    float lightRad = Math.max(w, h) * 0.6f;
    Fill.light(w / 2, h / 2, lightVerts, lightRad, Tmp.c1.set(ogreen).a(0.2f), Color.clear);
    Draw.flush();
  }
}
