/* (C) 2025 */
package oxygen.ui.draw;


import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;
import oxygen.ui.*;

public class SlideBackgroundDraw<T extends Element> extends UIDraw<T> {
  Floatp progress;
  Drawable background;
  Direction direction;

  public SlideBackgroundDraw(Drawable drawable, Direction direction, Floatp progress) {
    this.background = drawable;
    this.progress = progress;
    this.direction = direction;
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
    Direction.draw(background, data, direction, progress.get());
  }
}
