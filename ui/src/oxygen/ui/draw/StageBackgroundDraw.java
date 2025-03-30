/* (C) 2025 */
package oxygen.ui.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.style.*;

public class StageBackgroundDraw<T extends Element> extends UIDraw<T> {
  Drawable background;
  private static final Vec2 tmpPosition = new Vec2();
  private static final Vec2 tmpSize = new Vec2();

  public StageBackgroundDraw(Drawable background) {
    this.background = background;
  }

  @Override
  public void draw(T data) {
    if (this.background == null)
      return;
    Scene stage = data.getScene();
    data.stageToLocalCoordinates(tmpPosition.set(0, 0));
    data.stageToLocalCoordinates(tmpSize.set(stage.getWidth(), stage.getHeight()));
    drawStageBackground(data, data.x + tmpPosition.x, data.y + tmpPosition.y, data.x + tmpSize.x,
        data.y + tmpSize.y);
  }

  protected void drawStageBackground(T data, float x, float y, float width, float height) {
    Color color = data.color;
    Draw.color(color.r, color.g, color.b, color.a);
    background.draw(x, y, width, height);
  }
}
