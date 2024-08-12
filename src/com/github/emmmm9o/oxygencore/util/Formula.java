package com.github.emmmm9o.oxygencore.util;

import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.ui.ItemDisplay;

/**
 * Formula
 */
public class Formula {
  public float capacity;
  public float usage;
  public boolean buffered = false;
  public float craftTime;
  public ItemStack[] inputs;
  public ItemStack[] outputs;

  public Formula(ItemStack[] in, ItemStack[] out,
      float use, float craftTime) {
    inputs = in;
    outputs = out;
    usage = use;
    capacity = 0f;
    this.craftTime = craftTime;
  }

  public void display(Table t) {
    t.table(itemDis -> {
      itemDis.table(inputDis -> {
        inputDis.left();
        for (int i = 0; i < this.inputs.length; i++) {
          if (i % 6 == 0) {
            inputDis.row();
          }
          var it = this.inputs[i];
          inputDis.add(new ItemDisplay(it.item, it.amount, false)).pad(5);
        }
      }).left().growY();
      itemDis.image(Icon.right).size(StyleManager.ButtonSize);
      itemDis.table(outputDis -> {
        outputDis.right();
        for (int i = 0; i < this.outputs.length; i++) {
          if (i % 6 == 0) {
            outputDis.row();
          }
          var it = this.outputs[i];
          outputDis.add(new ItemDisplay(it.item, it.amount, false)).pad(5);
        }
      }).right().growY();
    }).left().pad(10).growX();
    t.table(power -> {
      power.image(Icon.power).color(Pal.power);
      power.add(Float.toString(
          this.usage * 60));
    }).right().grow().pad(10f);

  }
}
