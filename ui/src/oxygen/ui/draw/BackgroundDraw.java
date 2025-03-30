/* (C) 2025 */
package oxygen.ui.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;

public class BackgroundDraw<T extends Element> extends UIDraw<T> {
  Drawable background;

  public BackgroundDraw(Drawable background) {
    this.background = background;
  }

  @Override
  public void draw(T data) {
    drawBackground(data);
  }

  protected void drawBackground(T data) {
    if (background == null)
      return;
    Color color = data.color;
    Draw.color(color.r, color.g, color.b, color.a);
    background.draw(data.x, data.y, data.getWidth(), data.getHeight());
  }
}
