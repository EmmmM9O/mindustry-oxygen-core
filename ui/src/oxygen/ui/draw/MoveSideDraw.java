/* (C) 2025 */
package oxygen.ui.draw;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.util.Align;
import oxygen.ui.*;

public class MoveSideDraw<T extends Element> extends UIDraw<T> {
  Floatp progress;
  Drawable background;
  Direction direction;
  float distance, to, depth;

  public MoveSideDraw(Drawable drawable, float distance, float to, float depth, Direction direction,
      Floatp progress) {
    this.background = drawable;
    this.progress = progress;
    this.direction = direction;
    this.distance = distance;
    this.to = to;
    this.depth = depth;
  }

  @Override
  public void draw(T data) {
    drawBackground(data);
  }

  protected void drawBackground(T data) {
    if (background == null)
      return;
    if (progress.get() == 0)
      return;
    Color color = data.color;
    Draw.color(color.r, color.g, color.b, color.a);
    switch (direction) {
      case right:
        background.draw(data.x - depth - to - distance + progress.get() * distance, data.y, depth,
            data.getHeight());
        break;
      case left:
        background.draw(data.getX(Align.right) + to + distance - progress.get() * distance, data.y,
            depth, data.getHeight());
        break;
      case up:
        background.draw(data.x, data.y - depth - to - distance + progress.get() * distance,
            data.getWidth(), depth);
        break;
      case down:
        background.draw(data.x, data.y + to + distance - progress.get() * distance, data.getWidth(),
            depth);
        break;
    }
  }
}
