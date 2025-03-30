/* (C) 2025 */
package oxygen.ui;

import arc.func.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import oxygen.ui.draw.*;

public class UIDraws {
  public static UIDraw<OButton> buttonDraw(UIDraw<OButton> common, UIDraw<OButton> over,
      UIDraw<OButton> pressed, UIDraw<OButton> disabled) {
    return new ButtonDraw(common, over, pressed, disabled);
  }

  public static <T extends Element> UIDraw<T> loadDraw(Cons<T> func) {
    return new UIDraw<T>() {
      @Override
      public void load(T data) {
        func.get(data);
      }
    };
  }

  public static <T extends Element> UIDraw<T> drawer(Cons<T> func) {
    return new UIDraw<T>() {
      @Override
      public void draw(T data) {
        func.get(data);
      }
    };
  }

  public static <T extends Element> UIDraw<T> actDraw(Cons2<T, Float> func) {
    return new UIDraw<T>() {
      @Override
      public void act(T data, float delta) {
        func.get(data, delta);
      }
    };
  }

  @SafeVarargs
  public static <T extends Element> UIDraw<T> combineDraw(UIDraw<T>... draws) {
    return new UIDraw<>() {
      @Override
      public void draw(T data) {
        for (UIDraw<T> draw : draws) {
          draw.draw(data);
        }
      }

      @Override
      public void load(T data) {
        for (UIDraw<T> draw : draws) {
          draw.load(data);
        }
      }

      @Override
      public void act(T data, float delta) {
        for (UIDraw<T> draw : draws) {
          draw.act(data, delta);
        }
      }
    };
  }

  public static <T extends Element> TimeDraw<T> timeDraw(float max, Boolf<T> over,
      Func<TimeDraw<T>, UIDraw<T>> func) {
    return new TimeDraw<>(max, over, func);
  }

  public static <T extends Element> TimeDraw<T> timeDraw(float max, Boolf<T> over, UIDraw<T> draw) {
    return new TimeDraw<>(max, over, draw);
  }

  public static <T extends Element> TimeDraw<T> timeDraw(float startMax, float endMax,
      Boolf<T> over, Func<TimeDraw<T>, UIDraw<T>> func) {
    return new TimeDraw<>(startMax, endMax, over, func);
  }

  public static TimeDraw<OButton> overTimeDraw(float max,
      Func<TimeDraw<OButton>, UIDraw<OButton>> func) {
    return new TimeDraw<>(max, button -> button.isOver(), func);
  }

  public static TimeDraw<OButton> overTimeDraw(float startMax, float endMax,
      Func<TimeDraw<OButton>, UIDraw<OButton>> func) {
    return new TimeDraw<>(startMax, endMax, button -> button.isOver(), func);
  }

  public static Floatp timeProgress(Interp interp, TimeDraw<?> self) {
    return () -> interp.apply(Math.min(1f, self.progress));
  }

  public static Floatp timeProgress(Interp sInterp, Interp eInterp, TimeDraw<?> self) {
    return () -> (self.enabled ? sInterp : eInterp).apply(Math.min(1f, self.progress));
  }

  public static <T extends Element> SlideBackgroundDraw<T> slideBackgroundDraw(Direction direction,
      Drawable drawable, Floatp func) {
    return new SlideBackgroundDraw<>(drawable, direction, func);
  }

  public static <T extends Element> MoveSideDraw<T> moveSideDraw(float distance, float to,
      float depth, Direction direction, Drawable drawable, Floatp func) {
    return new MoveSideDraw<>(drawable, distance, to, depth, direction, func);
  }

  public static TimeDraw<OButton> overTimeBackgroundDraw(Direction direction, Drawable drawable,
      float duration, Floatp func) {
    return overTimeDraw(duration, self -> slideBackgroundDraw(direction, drawable, func));
  }

  public static UIDraw<OButton> overDraw(Cons<OButton> func) {
    return drawer(button -> {
      if (button.isOver())
        func.get(button);
    });
  }

  @SafeVarargs
  public static <T extends Element> Cons<T> combineCons(Cons<T>... funcs) {
    return t -> {
      for (Cons<T> func : funcs) {
        func.get(t);
      }
    };
  }

  public static <T extends Table> Cons<T> leftText(CharSequence text) {
    return leftText(text, 15f);
  }

  public static <T extends Table> Cons<T> leftText(CharSequence text, float marginLeft) {
    return tab -> {
      tab.add(text).marginLeft(marginLeft).left();
      tab.add().grow();
    };
  }

  public static <T extends Table> Cons<T> leftText(CharSequence text, Label.LabelStyle style, float marginLeft) {
    return tab -> {
      tab.add(text, style).marginLeft(marginLeft).left();
      tab.add().grow();
    };
  }
}
