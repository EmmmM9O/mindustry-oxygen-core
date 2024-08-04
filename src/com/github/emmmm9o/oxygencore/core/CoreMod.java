package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.ui.StyleManager;
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
    StyleManager.init();
    StyleManager.load();
    Manager.init();
    var window = new Window();
    window.resize(300f, 200f);
    window.show();
    window.setPosition(300f, 300f);
  }
}
