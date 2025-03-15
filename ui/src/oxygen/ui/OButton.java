/* (C) 2025 */
package oxygen.ui;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.pooling.*;
import oxygen.ui.draw.*;
import static arc.Core.*;

public class OButton extends Table implements Disableable {
  boolean isChecked, isDisabled;
  Boolp disabledProvider;
  private ClickListener clickListener;
  private boolean programmaticChangeEvents;
  private UIDraw<OButton> drawer;

  private void initialize() {
    this.touchable = Touchable.enabled;
    addListener(clickListener = new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (isDisabled())
          return;
        setChecked(!isChecked, true);
      }
    });
    addListener(new HandCursorListener());
    if (drawer != null)
      drawer.load(this);
  }

  public OButton(UIDraw<OButton> drawer) {
    this.drawer = drawer;
    initialize();
  }

  @Override
  public void setDisabled(boolean isDisabled) {
    this.isDisabled = isDisabled;
  }

  @Override
  public boolean isDisabled() {
    return this.isDisabled;
  }

  public void setDisabled(Boolp prov) {
    this.disabledProvider = prov;
  }

  public void toggle() {
    setChecked(!isChecked);
  }

  public boolean isChecked() {
    return isChecked;
  }

  public void setChecked(boolean isChecked) {
    setChecked(isChecked, programmaticChangeEvents);
  }

  public boolean isPressed() {
    return clickListener.isVisualPressed();
  }

  public boolean isOver() {
    return clickListener.isOver();
  }

  public ClickListener getClickListener() {
    return clickListener;
  }

  @Override
  public float getMinWidth() {
    return getPrefWidth();
  }

  @Override
  public float getMinHeight() {
    return getPrefHeight();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    if (disabledProvider != null) {
      setDisabled(disabledProvider.get());
    }
    if (drawer != null)
      drawer.act(this, delta);
  }

  void setChecked(boolean isChecked, boolean fireEvent) {
    if (this.isChecked == isChecked)
      return;
    this.isChecked = isChecked;

    if (fireEvent) {
      ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
      if (fire(changeEvent))
        this.isChecked = !isChecked;
      Pools.free(changeEvent);
    }
  }

  public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
    this.programmaticChangeEvents = programmaticChangeEvents;
  }

  @Override
  public void draw() {
    validate();
    super.draw();
    boolean isPressed = isPressed();
    if (drawer != null)
      drawer.draw(this);
    Scene stage = getScene();
    if (stage != null && stage.getActionsRequestRendering()
        && isPressed != clickListener.isPressed())
      Core.graphics.requestRendering();
  }

  public boolean childrenPressed() {
    boolean[] b = {false};
    Vec2 v = new Vec2();

    forEach(element -> {
      element.stageToLocalCoordinates(v.set(input.mouseX(), input.mouseY()));
      if (element instanceof Button
          && (((Button) element).getClickListener().isOver(element, v.x, v.y))) {
        b[0] = true;
      }
    });

    return b[0];
  }
}
