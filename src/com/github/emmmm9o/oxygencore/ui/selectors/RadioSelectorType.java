package com.github.emmmm9o.oxygencore.ui.selectors;

import com.github.emmmm9o.oxygencore.ui.TipTable;

import arc.func.Cons;
import arc.func.Func;
import arc.math.geom.Vec2;
import arc.struct.Seq;

/**
 * RadioSelectorType
 */
public class RadioSelectorType<T extends Selectable, D> extends SelectorType<T, D> {

  public RadioSelectorType(String name, Func<D, Seq<T>> builder, Class<?> dataClass) {
    super(name, builder, dataClass);
    selector_builder = (a, b, c, d) -> (Selector) (new RadioSelector(a, b, c, d, this));
  }

  public Selector create_radio(Func<TipTable, Vec2> positioner, Cons<T> callback, D data) {
    // return selector_builder.get(positioner, callback, list_builder.get(data),
    // data);
    return super.create(positioner, list -> {
      if (list.size == 0)
        callback.get(null);
      else
        callback.get(list.get(0));
    }, data);
  }
  
  public Selector create_radio(Func<TipTable, Vec2> positioner, Cons<T> callback, D data, int defaultT) {
    var selector = create_radio(positioner, callback, data);
    selector.select(selector.list.get(defaultT));
    return selector;
  }

  public Selector create_radio(Func<TipTable, Vec2> positioner, Cons<T> callback, D data, T defaultT) {
    var selector = create_radio(positioner, callback, data);
    selector.select(selector.list.find(t -> t.isSame(defaultT)));
    return selector;
  }

  public class RadioSelector extends Selector {
    public RadioSelector(Func<TipTable, Vec2> positioner, Cons<Seq<T>> callback, Seq<T> list, D data,
        SelectorType<T, D> type) {
      super(positioner, callback, list, data, type);
    }

    @Override
    public void select(T t) {
      if (list.contains(t)) {
        if (!t.isSelected()) {
          if (selected.size != 0) {
            selected.each(tr -> {
              tr.select();
            });
            selected.clear();
          }
          t.select();
          selected.add(t);
        } else {
          t.select();
          selected.remove(t);
        }
      }
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
