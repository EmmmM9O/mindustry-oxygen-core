package oxygen.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import oxygen.ui.draw.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class OCDialog extends Table {
  private static Prov<Action> defaultShowAction = () -> Actions.sequence(Actions.alpha(0),
      Actions.fadeIn(0.4f, Interp.fade)),
      defaultHideAction = () -> Actions.fadeOut(0.4f, Interp.fade);
  protected InputListener ignoreTouchDown = new InputListener() {
    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
      event.cancel();
      return false;
    }
  };
  public boolean isModal = true;
  Element previousKeyboardFocus, previousScrollFocus;
  FocusListener focusListener;
  protected boolean wasPaused;
  protected boolean shouldPause = true;

  public @Nullable UIDraw<OCDialog> drawer;

  public OCDialog() {
    this(null);
  }

  public OCDialog(@Nullable UIDraw<OCDialog> draw) {
    this.drawer = draw;
    this.touchable = Touchable.enabled;
    setClip(true);
    setFillParent(true);
    addCaptureListener(new InputListener() {
      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
        toFront();
        return false;
      }
    });
    setOrigin(Align.center);
    focusListener = new FocusListener() {
      @Override
      public void keyboardFocusChanged(FocusEvent event, Element actor, boolean focused) {
        if (!focused)
          focusChanged(event);
      }

      @Override
      public void scrollFocusChanged(FocusEvent event, Element actor, boolean focused) {
        if (!focused)
          focusChanged(event);
      }

      private void focusChanged(FocusEvent event) {
        Scene stage = getScene();
        if (isModal && stage != null && stage.root.getChildren().size > 0
            && stage.root.getChildren().peek() == OCDialog.this) {
          Element newFocusedActor = event.relatedActor;
          if (newFocusedActor != null && !newFocusedActor.isDescendantOf(OCDialog.this) &&
              !(newFocusedActor.equals(previousKeyboardFocus) || newFocusedActor.equals(previousScrollFocus)))
            event.cancel();
        }
      }
    };
    shown(this::updateScrollFocus);
    hidden(() -> {
      if (shouldPause && state.isGame() && !net.active() && !wasPaused) {
        state.set(State.playing);
      }
    });
    shown(() -> {
      if (shouldPause && state.isGame() && !net.active()) {
        wasPaused = state.is(State.paused);
        state.set(State.paused);
      }
    });
    if (this.drawer != null) {
      this.drawer.load(this);
    }
  }

  public OCDialog show() {
    return show(Core.scene);
  }

  public OCDialog show(Scene stage) {
    show(stage, defaultShowAction.get());
    centerWindow();
    return this;
  }

  public void centerWindow() {
    setPosition(Math.round(((Core.scene.getWidth() - scene.marginLeft - scene.marginRight) - getWidth()) / 2),
        Math.round(((Core.scene.getHeight() - scene.marginTop - scene.marginBottom) - getHeight()) / 2));
  }

  public OCDialog show(Scene stage, Action action) {
    setOrigin(Align.center);
    setClip(false);
    setTransform(true);

    this.fire(new VisibilityEvent(false));

    clearActions();
    removeCaptureListener(ignoreTouchDown);
    previousKeyboardFocus = null;
    Element actor = stage.getKeyboardFocus();
    if (actor != null && !actor.isDescendantOf(this))
      previousKeyboardFocus = actor;

    previousScrollFocus = null;
    actor = stage.getScrollFocus();
    if (actor != null && !actor.isDescendantOf(this))
      previousScrollFocus = actor;
    pack();
    stage.add(this);
    stage.setKeyboardFocus(this);
    stage.setScrollFocus(this);

    if (action != null)
      addAction(action);
    pack();

    return this;
  }

  public boolean isShown() {
    return getScene() != null;
  }

  public void toggle() {
    if (isShown()) {
      hide();
    } else {
      show();
    }
  }

  public void hide() {
    if (!isShown())
      return;
    setOrigin(Align.center);
    setClip(false);
    setTransform(true);

    hide(defaultHideAction.get());
  }

  public void hide(Action action) {
    this.fire(new VisibilityEvent(true));

    Scene stage = getScene();
    if (stage != null) {
      removeListener(focusListener);
      if (previousKeyboardFocus != null && previousKeyboardFocus.getScene() == null)
        previousKeyboardFocus = null;
      Element actor = stage.getKeyboardFocus();
      if (actor == null || actor.isDescendantOf(this))
        stage.setKeyboardFocus(previousKeyboardFocus);

      if (previousScrollFocus != null && previousScrollFocus.getScene() == null)
        previousScrollFocus = null;
      actor = stage.getScrollFocus();
      if (actor == null || actor.isDescendantOf(this))
        stage.setScrollFocus(previousScrollFocus);
    }
    if (action != null) {
      addCaptureListener(ignoreTouchDown);
      addAction(Actions.sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.remove()));
    } else
      remove();
  }

  public void updateScrollFocus() {
    boolean[] done = { false };

    Core.app.post(() -> forEach(child -> {
      if (done[0])
        return;

      if (child instanceof ScrollPane) {
        Core.scene.setScrollFocus(child);
        done[0] = true;
      }
    }));
  }

  public boolean isModal() {
    return isModal;
  }

  public void setModal(boolean isModal) {
    this.isModal = isModal;
  }

  public void shown(Runnable run) {
    addListener(new VisibilityListener() {
      @Override
      public boolean shown() {
        run.run();
        return false;
      }
    });
  }

  public void hidden(Runnable run) {
    addListener(new VisibilityListener() {
      @Override
      public boolean hidden() {
        run.run();
        return false;
      }
    });
  }

  public static void setHideAction(Prov<Action> prov) {
    defaultHideAction = prov;
  }

  public static void setShowAction(Prov<Action> prov) {
    defaultShowAction = prov;
  }

  protected void onResize(Runnable run) {
    Events.on(ResizeEvent.class, event -> {
      if (isShown() && sceneGetDialog() == this) {
        run.run();
      }
    });
  }

  public static OCDialog sceneGetDialog() {
    if (Core.scene.getKeyboardFocus() instanceof OCDialog) {
      return (OCDialog) Core.scene.getKeyboardFocus();
    } else if (Core.scene.getScrollFocus() instanceof OCDialog) {
      return (OCDialog) Core.scene.getScrollFocus();
    }
    return null;
  }

  @Override
  public void draw() {
    super.draw();
    if (drawer != null)
      drawer.draw(this);
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (drawer != null)
      drawer.act(this, delta);
  }
}
