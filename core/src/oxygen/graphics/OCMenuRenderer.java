/* (C) 2025 */
package oxygen.graphics;


import oxygen.graphics.universe.*;


public class OCMenuRenderer implements OCMenuRendererI {
  public BlackHoleRenderer blackHoleRenderer;

  public OCMenuRenderer() {
    blackHoleRenderer = new BlackHoleRenderer();
  }

  @Override
  public void render() {
    blackHoleRenderer.render();
  }

  @Override
  public void dispose() {
    blackHoleRenderer.dispose();
  }
}
