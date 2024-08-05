package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.content.OxygenBlocks;
import com.github.emmmm9o.oxygencore.ui.StyleManager;
import com.github.emmmm9o.oxygencore.ui.Window;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import arc.Events;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class CoreMod extends Mod {
  @Override
  public void init() {
    Events.on(EventType.ClientLoadEvent.class, event -> {
      Time.runTask(10f, () -> {
        loadUI();
      });
    });
  }

  @Override
  public void loadContent() {
    OxygenBlocks.init();
  }

  public void loadUI() {
    StyleManager.init();
    StyleManager.load();
    Manager.init();
    var window = new Window() {
      @Override
      public void drawStatus(Table table) {
        table.image(Icon.eye);
      }

      @Override
      public void drawBody(Table table) {
        table.image(Icon.eye);
      }
    };
    window.init();
    window.resize(300f, 200f);
    window.setStart(300f, 300f);
    window.show();
  }
}
