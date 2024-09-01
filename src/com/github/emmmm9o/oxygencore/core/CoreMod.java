package com.github.emmmm9o.oxygencore.core;

import com.github.emmmm9o.oxygencore.content.OxygenBlocks;

import arc.Events;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class CoreMod extends Mod {
  @Override
  public void init() {
    Manager.content.init();
    Manager.content.load();
    Manager.content.log();
  }

  public CoreMod() {
    Manager.init();
    Events.on(EventType.ClientLoadEvent.class, event -> {
      Time.runTask(10f, () -> {
        loadUI();
      });
    });
  }

  @Override
  public void loadContent() {
    // All the OContentType should be load during loadContent
    Manager.initContent();
    OxygenBlocks.load();
  }

  public void loadUI() {
    Manager.initUI();
  }
}
