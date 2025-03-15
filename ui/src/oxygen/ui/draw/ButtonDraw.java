/* (C) 2025 */
package oxygen.ui.draw;

import oxygen.ui.*;

/**
 * ButtonDraw
 */
public class ButtonDraw extends UIDraw<OButton> {
  UIDraw<OButton> overDrawer, disabledDrawer, commonDrawer, pressedDrawer;

  @Override
  public void draw(OButton data) {
    if (commonDrawer != null)
      commonDrawer.draw(data);
    if (overDrawer != null && data.isOver())
      overDrawer.draw(data);
    if (disabledDrawer != null && data.isDisabled())
      disabledDrawer.draw(data);
    if (pressedDrawer != null && data.isPressed())
      pressedDrawer.draw(data);
  }

  @Override
  public void load(OButton data) {
    commonDrawer.load(data);
  }

  @Override
  public void act(OButton data, float delta) {
    commonDrawer.act(data, delta);
  }

  public ButtonDraw() {}

  public ButtonDraw(UIDraw<OButton> common, UIDraw<OButton> over, UIDraw<OButton> pressed,
      UIDraw<OButton> disabled) {
    this.commonDrawer = common;
    this.overDrawer = over;
    this.disabledDrawer = disabled;
    this.pressedDrawer = pressed;
  }
}
