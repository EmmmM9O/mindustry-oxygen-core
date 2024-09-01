
package com.github.emmmm9o.oxygencore.ui;

import arc.func.Cons;
import arc.func.Func2;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;

/**
 * Radio
 */
public class Radio<T> extends Table {
  public Seq<T> datas;
  public Cons<T> resultCons;
  public Func2<T, Table, Table> contentCons;
  public boolean row;
  public T current;
  public ObjectMap<T, Table> dataTables;

  public Radio(Seq<T> selectors, T current, Drawable background, Func2<T, Table, Table> contentCons, Cons<T> resultCons,
      boolean row) {
    super(background);
    this.datas = selectors;
    this.contentCons = contentCons;
    this.resultCons = resultCons;
    this.row = row;
    this.current = current;
    this.dataTables = new ObjectMap<>();
    rebuildTable();
  }

  public void changeTo(T data, boolean tigger) {
    if (data != current) {
      dataTables.get(current).setBackground(StyleManager.style.buttonUnselect);
      current = data;
      dataTables.get(current).setBackground(StyleManager.style.buttonSelect);
      if (tigger)
        resultCons.get(current);
    }
  }

  public void changeTo(T data) {
    changeTo(data, false);
  }

  public void rebuildTable() {
    this.clearChildren();
    for (var data : datas) {
      var tab = contentCons.get(data, this);
      dataTables.put(data, tab);
      tab.addListener(new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
          if (data != current) {
            changeTo(data, true);
          }
          return true;
        }
      });
      tab.touchable = Touchable.enabled;
      if (current == data) {
        tab.setBackground(StyleManager.style.buttonSelect);
      } else {
        tab.setBackground(StyleManager.style.buttonUnselect);
      }
      if (row)
        row();
    }
  }
}
