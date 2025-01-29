package oxygen.ui.fragments;

import arc.scene.*;
import arc.scene.ui.layout.*;
import mindustry.ui.fragments.*;

public class OCMenuFragment extends MenuFragmentI {

  @Override
  public void build(Group parent) {
    Group group = new WidgetGroup();
    group.setFillParent(true);
    group.visible(() -> true);
    parent.addChild(group);

    parent = group;
  }
}
