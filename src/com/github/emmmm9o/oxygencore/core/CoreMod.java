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
    Events.on(EventType.ClientLoadEvent.class, event -> {
      Time.runTask(10f, () -> {
        loadUI();
      });
    });
  }

  @Override
  public void loadContent() {

  }

  public void loadUI() {
    Manager.init();
    StyleMananger.init();
    StyleMananger.load();
    var window = new Window();
    window.setPosition(100f, 100f);
    window.resize(300f, 200f);
    window.show();
  }
}
