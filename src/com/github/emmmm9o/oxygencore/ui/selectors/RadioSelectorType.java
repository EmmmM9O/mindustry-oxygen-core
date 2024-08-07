package com.github.emmmm9o.oxygencore.ui.selectors;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import arc.math.geom.Vec2;
import arc.struct.Seq;

/**
 * RadioSelectorType
 */
public class RadioSelectorType<T extends Selectable, D> extends SelectorType<T, D> {

  public RadioSelectorType(String name, Func<D, Seq<T>> builder) {
    super(name, builder);
  }

  public Selector create_radio(Prov<Vec2> positioner, Cons<T> callback, D data) {
    // return selector_builder.get(positioner, callback, list_builder.get(data),
    // data);
    return super.create(positioner, list -> {
      if (list.size == 0)
        callback.get(null);
      else
        callback.get(list.get(0));
    }, data);
  }

  public class RadioSelector extends Selector {
    @Override
    public void select(T t) {
      super.select(t);
      check();
    }

    @Override
    public void check() {
      if (selected.size > 1) {
        var S = selected.get(selected.size - 1);
        clearS();
        select(S);
      }
    }
  }
}
