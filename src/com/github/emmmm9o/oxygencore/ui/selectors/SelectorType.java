package com.github.emmmm9o.oxygencore.ui.selectors;

import java.lang.reflect.Constructor;

import com.github.emmmm9o.oxygencore.func.Func4;
import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.ui.TipTable;
import com.github.emmmm9o.oxygencore.util.Reflection;

import arc.Core;
import arc.func.Func;
import arc.func.Prov;
import arc.func.Cons;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Structs;
import mindustry.Vars;
import mindustry.gen.Icon;

public class SelectorType<T extends Selectable, D> {
  public Func<D, Seq<T>> list_builder;
  public Func4<Prov<Vec2>, Cons<Seq<T>>, Seq<T>, D, Selector> selector_builder;
  public String name;
  public String localizedName;
  public @Nullable Class<?> subclass;

  public void initSeletor() {
    try {
      Class<?> current = getClass();

      if (current.isAnonymousClass()) {
        current = current.getSuperclass();
      }

      subclass = current;
      while (selector_builder == null && SelectorType.class.isAssignableFrom(current)) {
        Class<?> type = Structs.find(
            current.getDeclaredClasses(), t -> Selector.class.isAssignableFrom(t) && !t.isInterface());
        if (type != null) {
          Constructor<? extends Selector> cons = (Constructor<? extends Selector>) type
              .getDeclaredConstructor(type.getDeclaringClass());
          selector_builder = (a, b, c, d) -> {
            try {
              return cons.newInstance(a, b, c, d, this);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };
        }
        current = current.getSuperclass();
      }

    } catch (Throwable ignored) {

    }
    if (selector_builder == null) {
      selector_builder = (a, b, c, d) -> null;
      // only for debug
      throw new RuntimeException("Error class " + getClass().toString());
    }
  }

  public SelectorType(String name, Func<D, Seq<T>> builder) {
    this(name);
    this.list_builder = builder;
  }

  public SelectorType(String name) {
    this.name = name;
    this.localizedName = Core.bundle.get("selector." + this.name + ".name", this.name);
    initSeletor();
  }

  public Selector create(Prov<Vec2> positioner, Cons<Seq<T>> callback, D data) {
    return selector_builder.get(positioner, callback, list_builder.get(data), data);
  }

  public class Selector extends TipTable {
    public SelectorType<T, D> type;
    public Seq<T> list;
    public D data;
    public Seq<T> selected;
    public Cons<Seq<T>> callback;
    public Class<T> classType;
    public TipTable infoTip;
    public Selectable lastSelected;

    public void buildTable(Table table) {
      table.table(StyleManager.style.titleBarBackground, top -> {
        top.table(StyleManager.style.titleTextBackground, text -> {
          text.add(localizedName).height(48).growX()
              .get().setAlignment(Align.center);
        }).height(48).growX().left();
        top.table(buttons -> {
          buttons.button(Icon.trash, StyleManager.style.windowButtons,
              () -> {
                clearS();
              }).size(48).uniform();
          buttons.button(Icon.save, StyleManager.style.windowButtons,
              () -> {
                callback.get(selected);
                hide();
              }).size(48).uniform();
          buttons.button(Icon.copy, StyleManager.style.windowButtons,
              () -> {
                Core.app.setClipboardText(copy());
              }).size(48).uniform();
          buttons.button(Icon.paste, StyleManager.style.windowButtons,
              () -> {
                paste(Core.app.getClipboardText());
              }).size(48).uniform();
        }).height(48).right();
      }).height(48).uniformX().growX();
      table.table(cont -> {
        var index = 0;
        for (var opt : list) {
          opt.displayIcon(cont, () -> {
            select(opt);
            opt.select();
            displayInfoTable(opt);
          });
          index++;
          if (index >= 10) {
            cont.row();
          }
        }
      }).uniformX().growX();
    }

    public void clearS() {
      for (var s : selected) {
        s.select();
      }
      selected.clear();
    }

    public void buildInfoTable(Selectable current, Table table) {
      table.table(main -> {
        current.display(main);
      }).grow();
      table.table(footer -> {
        footer.label(() -> (current.isSelected() ? "selected" : "un selected"))
            .height(48).growX().get().setAlignment(Align.center);
      }).height(48).growX();
    }

    public void displayInfoTable(Selectable current) {
      if (current == lastSelected) {
        if (infoTip.visible) {
          infoTip.hide();
        } else {
          infoTip.show();
        }
      } else {
        infoTip.clearChildren();
        infoTip.table(table -> {
          buildInfoTable(current, table);
        }).grow();
        if (!infoTip.visible) {
          infoTip.show();
        }
        lastSelected = current;
      }
    }

    public String copy() {
      var res = new StringBuilder();
      for (var t : selected) {
        res.append(t.write());
        res.append(",");
      }
      return res.toString();
    }

    public void paste(String str) {
      clearS();
      var r = str.split(",");
      if (r.length == 0) {
        Vars.ui.showErrorMessage("Error invalid content");
        return;
      }
      for (var s : r) {
        if (!s.isEmpty()) {
          var select = Reflection.construct(classType);
          if (!select.read(s)) {
            select.select();
            selected.add(select);
          } else {
            selected.clear();
            Vars.ui.showErrorMessage("Error invalid content");
            break;
          }
        }
      }
      check();
    }

    public void check() {

    }

    public void select(T t) {
      if (list.contains(t)) {
        t.select();
        if (!selected.contains(t)) {
          selected.add(t);
        } else {
          selected.remove(t);
        }
      }
    }

    public Selector() {
    }// it woudle not be used

    public Selector(Prov<Vec2> positioner, Cons<Seq<T>> callback, Seq<T> list, D data, SelectorType<T, D> type) {
      super(positioner, StyleManager.style.bodyBackground);
      this.list = list;
      this.data = data;
      this.type = type;
      this.callback = callback;
      classType = (Class<T>) list.get(0).getClass();
      infoTip = new TipTable(() -> {
        var dx = this.x + this.getWidth();
        var dy = this.y + this.getHeight() - infoTip.getHeight();
        return new Vec2(dx, dy);
      }, StyleManager.style.bodyBackground);
      infoTip.visible = false;
      table(table -> {
        buildTable(table);
      }).grow();
    }
  }
}