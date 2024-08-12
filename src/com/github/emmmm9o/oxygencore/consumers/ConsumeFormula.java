package com.github.emmmm9o.oxygencore.consumers;

import com.github.emmmm9o.oxygencore.blocks.MulticraftBlock.MulticraftBuild;
import com.github.emmmm9o.oxygencore.util.Formula;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.ui.ItemImage;
import mindustry.ui.ReqImage;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.Stats;

/**
 * ConsumeFormula
 */
public class ConsumeFormula extends ConsumePower {
  protected Seq<Formula> formulas;

  public ConsumeFormula(Seq<Formula> formulas) {
    this.formulas = formulas;
  }

  @Override
  public void apply(Block block) {
    block.hasPower = true;
    block.consPower = this;
    block.hasItems = true;
  }

  @Override
  public boolean ignore() {
    return false;
  }

  @Override
  public void build(Building build, Table table) {
    table.table(c -> {
      MulticraftBuild bu = build.as();
      if(bu.select()==-1) return;
      var items = formulas.get(bu.select()).inputs;
      int i = 0;
      for (var stack : items) {
        c.add(new ReqImage(new ItemImage(stack.item.uiIcon, Math.round(stack.amount * multiplier.get(build))),
            () -> build.items.has(stack.item, Math.round(stack.amount * multiplier.get(build)))))
            .padRight(8);

        if (++i % 4 == 0)
          c.row();
      }

    }).left();
  }

  @Override
  public void trigger(Building build) {
    MulticraftBuild bu = build.as();
      if(bu.select()==-1) return;
    var items = formulas.get(bu.select()).inputs;
    for (var stack : items) {
      build.items.remove(stack.item, Math.round(stack.amount * multiplier.get(build)));
    }
  }

  @Override
  public float efficiency(Building build) {
    MulticraftBuild bu = build.as();
      if(bu.select()==-1) return 0;
    var items = formulas.get(bu.select()).inputs;
    return build.consumeTriggerValid() || build.items.has(items, multiplier.get(build)) ? build.power.status : 0f;
  }

  @Override
  public void display(Stats stats) {
  }

  public float requestedPower(Building entity) {
    MulticraftBuild bu = entity.as();
      if(bu.select()==-1) return 0;
    var form = formulas.get(bu.select());
    return form.buffered ? (1f - entity.power.status) * form.capacity : form.usage * (entity.shouldConsume() ? 1f : 0f);
  }
}
