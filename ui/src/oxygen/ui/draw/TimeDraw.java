/* (C) 2025 */
package oxygen.ui.draw;

import arc.func.*;
import arc.scene.*;

public class TimeDraw<T extends Element> extends UIDraw<T> {
  public float time, startMax = 100, endMax, progress;
  public boolean enabled;
  UIDraw<T> drawer;
  Boolf<T> over;

  public TimeDraw(float max, Boolf<T> over, UIDraw<T> drawer) {
    this.over = over;
    this.drawer = drawer;
    this.startMax = max;
    this.endMax = max;
  }

  public TimeDraw(float max, Boolf<T> over, Func<TimeDraw<T>, UIDraw<T>> func) {
    this(max, max, over, func);
  }

  public TimeDraw(float startMax, float endMax, Boolf<T> over, Func<TimeDraw<T>, UIDraw<T>> func) {
    this.over = over;
    this.startMax = startMax;
    this.endMax = endMax;
    this.drawer = func.get(this);
  }

  @Override
  public void load(T data) {
    time = 0;
    if (drawer != null)
      drawer.load(data);
  }

  @Override
  public void act(T data, float delta) {
    if (over.get(data)) {
      enabled = true;
      if (time >= startMax) {
        time = startMax;
      } else
        time += delta;
      progress = time / startMax;
    } else {
      if (time >= endMax)
        time = endMax;
      enabled = false;
      if (time > 0) {
        time -= delta;
      } else {
        time = 0;
      }
      progress = time / endMax;
    }
    if (drawer != null)
      drawer.act(data, delta);
  }

  @Override
  public void draw(T data) {
    if (drawer != null)
      drawer.draw(data);
  }
}
