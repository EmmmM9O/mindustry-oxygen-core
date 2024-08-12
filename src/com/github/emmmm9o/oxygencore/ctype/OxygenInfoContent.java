package com.github.emmmm9o.oxygencore.ctype;

import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatValue;
import mindustry.world.meta.Stats;

public abstract class OxygenInfoContent extends OxygenMappableContent {
  public String localizedName;
  public @Nullable String description, details;
  public Stats stats = new Stats();
  public TextureRegion fullIcon;
  public TextureRegion uiIcon;

  public OxygenInfoContent(String name, LoadedMod mod) {
    super(name, mod);
    this.localizedName = Core.bundle.get(getContentType().name + "." + this.name + ".name", this.name);
    this.description = Core.bundle.getOrNull(getContentType().name + "." + this.name + ".description");
    this.details = Core.bundle.getOrNull(getContentType().name + "." + this.name + ".details");
  }

  @Override
  public void loadIcon() {
    fullIcon = Core.atlas.find(mod.name + "-" + getContentType().name + "-" + orginName,
        Core.atlas.find(getContentType().name + "-" + name + "-full",
            Core.atlas.find(name + "-full",
                Core.atlas.find(name,
                    Core.atlas.find(getContentType().name + "-" + name,
                        Core.atlas.find(name + "1"))))));

    uiIcon = Core.atlas.find(mod.name + "-" + getContentType().name + "-" + orginName + "-ui",
        Core.atlas.find(name + "-ui", Core.atlas.find(getContentType().name + "-" + name + "-ui", fullIcon)));
  }

  public String displayDescription() {
    return mod == null ? description : description + "\n" + Core.bundle.format("mod.display", mod.meta.displayName());
  }

  public void checkStats() {
    if (!stats.intialized) {
      setStats();
      stats.intialized = true;
    }
  }

  public void setStats() {

  }

  public Cons<Boolean> displaySelectIcon(Table table, Runnable onClick, boolean selected) {

    var button = table.button(new TextureRegionDrawable(uiIcon),
        selected ? StyleManager.style.selectedButton : StyleManager.style.windowButtons, () -> {

        }).size(64)
        .uniform();
    return select -> {
      if (button != null) {
        if (select) {
          button.get().setStyle(StyleManager.style.selectedButton);
        } else {
          button.get().setStyle(StyleManager.style.windowButtons);
        }
      }
    };
  }

  public Cell<?> displayIcon(Table table, Runnable onClick) {
    return table.button(new TextureRegionDrawable(uiIcon), StyleManager.style.windowButtons, onClick).size(64)
        .uniform();
  }

  public void display(Table table) {
    checkStats();
    table.table(title1 -> {
      title1.image(uiIcon).size(Vars.iconXLarge).scaling(Scaling.fit);
      title1.add("[accent]" + localizedName + "\n[gray]").padLeft(5);
    }).row();
    if (description != null) {
      var any = stats.toMap().size > 0;

      if (any) {
        table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
        table.row();
      }

      table.add("[lightgray]" + displayDescription()).wrap().fillX().padLeft(any ? 10 : 0).width(500f)
          .padTop(any ? 0 : 10).left();
      table.row();

      if (!stats.useCategories && any) {
        table.add("@category.general").fillX().color(Pal.accent);
        table.row();
      }
    }

    for (StatCat cat : stats.toMap().keys()) {
      OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);

      if (map.size == 0)
        continue;

      if (stats.useCategories) {
        table.add("@category." + cat.name).color(Pal.accent).fillX();
        table.row();
      }

      for (Stat stat : map.keys()) {
        table.table(inset -> {
          inset.left();
          inset.add("[lightgray]" + stat.localized() + ":[] ").left().top();
          Seq<StatValue> arr = map.get(stat);
          for (StatValue value : arr) {
            value.display(inset);
            inset.add().size(10f);
          }

        }).fillX().padLeft(10);
        table.row();
      }
    }

    if (details != null) {
      table.add("[gray]" + details).pad(6).padTop(20).width(400f).wrap().fillX();
      table.row();
    }

    displayExtra(table);

  }

  public void displayExtra(Table table) {

  }
}
