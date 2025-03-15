/* (C) 2025 */
package oxygen.ui;

import arc.math.geom.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.util.*;

/**
 * Direction
 */
public enum Direction {
  left, right, up, down;

  public static Vec2 getPos(Element element, Direction direction, float depth) {
    switch (direction) {
      case right:
        return new Vec2(element.x, element.y);
      case left:
        return new Vec2(element.getX(Align.right) - depth, element.y);
      case up:
        return new Vec2(element.x, element.y);
      case down:
        return new Vec2(element.x, element.getY(Align.top) - depth);
    }
    return null;
  }

  public static void draw(Drawable drawable, Element element, Direction direction, float progress) {
    switch (direction) {
      case right:
        drawable.draw(element.x, element.y, element.getWidth() * progress, element.getHeight());
        break;
      case left:
        drawable.draw(element.getX(Align.right) - element.getWidth() * progress, element.y,
            element.getWidth() * progress, element.getHeight());
        break;
      case up:
        drawable.draw(element.x, element.y, element.getWidth(), element.getHeight() * progress);
        break;
      case down:
        drawable.draw(element.x, element.getY(Align.top) - element.getHeight() * progress,
            element.getWidth(), element.getHeight() * progress);
        break;
    }
  }

  public static boolean isHorizontal(Direction direction) {
    return direction == left || direction == right;
  }

  public static boolean isVertical(Direction direction) {
    return direction == down || direction == up;
  }
}
