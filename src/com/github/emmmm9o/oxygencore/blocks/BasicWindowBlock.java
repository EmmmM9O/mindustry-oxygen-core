package com.github.emmmm9o.oxygencore.blocks;

import com.github.emmmm9o.oxygencore.ui.BlockWindow;
import com.github.emmmm9o.oxygencore.ui.WindowListener;

import arc.Core;
import arc.graphics.g2d.Lines;
import arc.graphics.Color;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;

public class BasicWindowBlock extends Block {
  public BasicWindowBlock(String name) {
    super(name);
  }

  public class BasicWindowBuild extends Building implements WindowListener {
    public BlockWindow window;


    @Override
    public void draw() {
      super.draw();
      drawWindowLine();
    }

    public void drawWindowLine() {
      if (this.window != null && this.window.visible) {
        Lines.stroke(2f, new Color(0f, 0f, 0f, 0.3f));
        var pos = Core.input.mouseWorld(window.x, window.y);
        Lines.line(x, y, pos.x, pos.y);
      }
    }

    public void showWindow() {
      if (window == null || window.closed) {
        initWindow();
        drawWindow();
      }
      if (window.visible)
        window.hide();
      else
        window.show();
    }

    public void initWindow() {
      window = new BlockWindow(this);
      window.init();
      window.resize(300f, 200f);
      setWindowStartPos();
    }

    public void onWindowClose() {

    }

    public void setWindowStartPos() {
      var pos = Core.input.mouseScreen(x, y);
      window.setStart(pos.x, pos.y);
    }

    public void setWindowPos() {
      var pos = Core.input.mouseScreen(x, y);
      window.setPosition(pos.x, pos.y);
    }

    public void setWindowPos(float x, float y) {
      var pos = Core.input.mouseScreen(x, y);
      window.setPosition(pos.x, pos.y);
    }

    public void drawWindow() {

    }

    @Override
    public void afterDestroyed() {
      if (window != null)
        window.close();
    }
  }
}
