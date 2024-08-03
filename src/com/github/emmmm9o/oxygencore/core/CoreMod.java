package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.ui.StyleMananger;
import com.github.emmmm9o.oxygencore.ui.Window;

import arc.Events;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class CoreMod extends Mod {
  @Override
  public void init() {

  }

  @Override
  public void loadContent() {
    StyleMananger.init();
    StyleMananger.load();
    Events.on(EventType.ClientLoadEvent.class, event -> {
      Time.runTask(10f, () -> {
        var window = new Window();
        window.center();
        window.show();
      });
    });
  }
}
