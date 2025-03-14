package oxygen.ui;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.pooling.*;
import oxygen.ui.draw.*;

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
    drawer.load(this);
  }

  public OButton() {
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
  public void act(float delta) {
    super.act(delta);
    if (disabledProvider != null) {
      setDisabled(disabledProvider.get());
    }
    this.drawer.act(this, delta);
  }

  @SuppressWarnings("unchecked")
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
    boolean isPressed = isPressed();
    this.drawer.draw(this);
    Scene stage = getScene();
    if (stage != null && stage.getActionsRequestRendering() && isPressed != clickListener.isPressed())
      Core.graphics.requestRendering();
  }

}
