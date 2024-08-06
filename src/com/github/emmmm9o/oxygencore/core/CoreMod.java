package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.content.OxygenBlocks;
import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.Events;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class CoreMod extends Mod {
  @Override
  public void init() {
    Events.on(EventType.ContentInitEvent.class, e -> {
      Manager.content.init();
      Manager.content.load();
      Manager.content.log();
    });
    Events.on(EventType.ClientLoadEvent.class, event -> {
      Time.runTask(10f, () -> {
        loadUI();
      });
    });

  }

  @Override
  public void loadContent() {
    Manager.initContent();
    OxygenBlocks.load();
  }

  public void loadUI() {
    StyleManager.init();
    StyleManager.load();
    Manager.initUI();

  }
}
