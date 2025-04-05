/* (C) 2025 */
package oxygen.graphics;

import oxygen.graphics.universe.*;
import static oxygen.core.OCVars.*;

public class OCMenuRenderer implements OCMenuRendererI {

  public OCMenuRenderer() {

  }

  @Override
  public void render() {
    renderer.blackHoleRenderer.render();
  }

  @Override
  public void dispose() {}
}
