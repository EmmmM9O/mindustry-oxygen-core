/* (C) 2025 */
package oxygen.graphics;


import arc.*;
import arc.graphics.g2d.*;

import static oxygen.ui.OCPal.*;

public class OCMenuRenderer implements OCMenuRendererI {
  @Override
  public void render() {
    Draw.color(obeige);
    Fill.crect(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    Draw.color();
  }

  @Override
  public void dispose() {

  }
}
