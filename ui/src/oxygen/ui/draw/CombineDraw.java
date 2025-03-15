/* (C) 2025 */
package oxygen.ui.draw;

import arc.scene.*;

public class CombineDraw<T extends Element> extends UIDraw<T> {
  UIDraw<T> firstDraw, secondDraw;

  public CombineDraw() {

  }

  public CombineDraw(UIDraw<T> first, UIDraw<T> second) {
    this.firstDraw = first;
    this.secondDraw = second;
  }

  @Override
  public void load(T data) {
    if (firstDraw != null)
      firstDraw.load(data);
    if (secondDraw != null)
      secondDraw.load(data);
  }


  @Override
  public void draw(T data) {
    if (firstDraw != null)
      firstDraw.draw(data);
    if (secondDraw != null)
      secondDraw.draw(data);
  }

  @Override
  public void act(T data, float delta) {
    if (firstDraw != null)
      firstDraw.act(data, delta);
    if (secondDraw != null)
      secondDraw.act(data, delta);
  }
}
