package com.github.emmmm9o.oxygencore.blocks;

import com.github.emmmm9o.oxygencore.consumers.ConsumeFormula;
import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.util.Formula;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Stat;

/**
 * MulticraftBlock
 */
public class MulticraftBlock extends IOBlock {
  public Seq<Formula> formulas;
  protected DrawBlock drawer = new DrawDefault();
  protected Short initSelect = -1;
  public float warmupSpeed = 0.019f;

  public MulticraftBlock(String name, int size) {
    super(name, size);
    sync = true;
    update=true;
    copyConfig = true;
    ambientSoundVolume = 0.03f;
    flags = EnumSet.of(BlockFlag.factory);
    ambientSound = Sounds.machine;
    config(Integer.class, (MulticraftBuild build, Integer data) -> {
      if (data < -1) {
        return;
      }
      if (data >= formulas.size) {
        return;
      }
      build.select = data.shortValue();
    });
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.add(
        Stat.output, table -> {
          table.row();
          for (var formula : formulas) {
            table.table(StyleManager.style.bodyBackground, t -> {
              formula.display(t);
            }).growX().pad(5);
            table.row();
          }
        });
  }

  @Override
  public void load() {
    super.load();
    drawer.load(this);
  }

  @Override
  public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
    drawer.drawPlan(this, plan, list);
  }

  @Override
  public TextureRegion[] icons() {
    return drawer.finalIcons(this);
  }

  @Override
  public void setBars() {
    addBar("health", entity -> new Bar("stat.health", Pal.health, entity::healthf).blink(Color.white));

    if (consPower != null) {
      addBar("power", (entity) -> {

        MulticraftBuild bu = entity.as();
        if (bu.select == -1) {
          return new Bar("bar.power", Pal.powerBar, () -> 0f);
        }
        boolean buffered = formulas.get(bu.select).buffered;
        float capacity = formulas.get(bu.select).capacity;
        return new Bar(
            (() -> buffered
                ? Core.bundle.format("bar.poweramount",
                    Float.isNaN(entity.power.status * capacity) ? "<ERROR>"
                        : UI.formatAmount((int) (entity.power.status * capacity)))
                : Core.bundle.get("bar.power")),
            () -> Pal.powerBar,
            () -> Mathf.zero(consPower.requestedPower(entity))
                && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f
                    ? 1f
                    : entity.power.status);
      });
    }
    if (hasItems && configurable) {
      addBar("items", entity -> new Bar(
          () -> Core.bundle.format("bar.items", entity.items.total()),
          () -> Pal.items,
          () -> (float) entity.items.total() / itemCapacity));
    }

    if (unitCapModifier != 0) {
      stats.add(Stat.maxUnits, (unitCapModifier < 0 ? "-" : "+") + Math.abs(unitCapModifier));
    }
  }

  public ConsumeFormula consumeFormula(Formula... forms) {
    formulas = new Seq<>();
    for (var f : forms) {
      formulas.add(f);
    }
    return consume(new ConsumeFormula(formulas));
  }

  public class MulticraftBuild extends IOBuild {
    public float progress;
    public float totalProgress;
    public float warmup;
    public Short select = -1;

    @Override
    public void created() {
	    super.created();
      select = initSelect;
    }

    @Override
    public void draw() {
      drawer.draw(this);
      drawWindowLine();
    }

    @Override
    public void drawLight() {
      super.drawLight();
      drawer.drawLight(this);
    }

    public float warmupTarget() {
      return 1f;
    }
    @Override
            public float getProgressIncrease(float baseTime) {
		                return super.getProgressIncrease(baseTime);
				        }
    @Override
    public boolean shouldConsume() {
      if (select == -1)
        return false;
      for (var i : formulas.get(select).outputs) {
        if (items.get(i.item) + i.amount > itemCapacity) {
          return false;
        }
      }
      return enabled;
    }

    public void dumpOutputs() {
	    if(select==-1) return;
      if (timer(timerDump, dumpTime / timeScale)) {
        for (var output : formulas.get(select).outputs) {
          dump(output.item);
        }
        for (var item : Vars.content.items()) {
          if (items.get(item) > 0) {
            if (!isNeeded(item)) {
              dump(item);
            }
          }
        }
      }
    }

    @Override
    public float progress() {
      return Mathf.clamp(progress);
    }

    @Override
    public float warmup() {
      return warmup;
    }

    @Override
    public float totalProgress() {
      return totalProgress;
    }

    @Override
    public boolean shouldAmbientSound() {
      return efficiency > 0;
    }
        @Override
	        public int getMaximumAccepted(Item item) {
			            return itemCapacity;
}

    public Seq<Formula> formula() {
      return formulas;
    }

    public Short select() {
      return select;
    }

    public void select(Short s) {
      this.select = s;
    }

    @Override
    public void updateTile() {
      if (select == -1) {
        progress = 0;
        warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
        dumpOutputs();
        return;
      }

      var formula = formulas.get(select);
      if ( efficiency > 0) {
        progress += getProgressIncrease(formula.craftTime);
        warmup = Mathf.approachDelta(warmup, warmupTarget(), warmupSpeed);
      } else {
        warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
      }
      totalProgress += warmup * Time.delta;

      if (progress >= 1f) {
        craft();
      }
      dumpOutputs();

    }

    public void craft() {
      consume();
      for (var output : formulas.get(select).outputs) {
        for (int i = 0; i < output.amount; i++) {
          offload(output.item);
        }

      }
      progress %= 1f;
    }

    public boolean isNeeded(Item item) {
	    if(select==-1)
		    return false;
      for (var input : formulas.get(select).inputs) {
        if (input.item == item)
          return true;
      }
      return false;
    }

    public boolean isOutput(Item item) {
      for (var output : formulas.get(select).inputs) {
        if (output.item == item)
          return true;
      }
      return false;
    }

    @Override
    public boolean canDumpGen(Building to, Item item) {
      return true;
    }

    @Override
    public boolean acceptItemGen(Building source, Item item) {
      return super.acceptItemGen(source, item) && isNeeded(item);
    }

    @Override
    public void writeT(Writes write) {
      write.s(select);
    }

    @Override
    public void readT(Reads read) {
      select = read.s();
    }
@Override
public void read(Reads read, byte revision){
super.read(read, revision);
progress = read.f();
warmup = read.f();
}
@Override
public void write(Writes write) {
super.write(write);
write.f(progress);write.f(warmup);
}
    @Override
    public void displayWindowExtra(Table tab) {
      tab.table(table -> {
        int index = 0;
        for (var formula : formulas) {
          final int finalInd = index;
          table.table(select == index ? Styles.accentDrawable : StyleManager.style.bodyBackground, t -> {
            formula.display(t);
          }).growX().pad(5).uniformX().name(Integer.toString(index)).update(ta -> {
            var ind = Integer.parseInt(ta.name);
            var tag = ind == select ? Styles.accentDrawable : StyleManager.style.bodyBackground;
            if (tag != ta.getBackground()) {
              ta.setBackground(tag);
            }
          }).get().addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
              if (select != finalInd)
                configure(finalInd);
              else
                configure(-1);
              return true;
            }
          });
          table.row();
          index++;
        }
      }).grow();
    }
  }
}
