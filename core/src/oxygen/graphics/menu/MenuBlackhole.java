/* (C) 2025 */
package oxygen.graphics.menu;

import arc.*;
import arc.fx.*;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.util.*;
import mindustry.*;
import oxygen.graphics.*;
import oxygen.graphics.OCShaders.*;
import oxygen.graphics.bloom.*;

import static oxygen.core.OCVars.*;

public class MenuBlackhole implements OCMenuRendererI {
  public Cubemap galaxy;
  public Texture colorMap, noise;
  public OCBloom bloom;
  public Camera3D cam;
  public FrameBuffer buffer, ray, ray2;
  public Mesh screen;
  public MenuBlackHoleShader shader;
  public FxFilter antialiasingFilter;
  public boolean rflag, init;
  float zoom = 1f, camLength = 2f, radius = 1.5f, camRadius = 6f, minZoom = 0.1f;

  public MenuBlackhole() {
    galaxy = renderer.universeRenderer.galaxy;
    bloom = renderer.universeRenderer.bloom;
    cam = renderer.universeRenderer.cam;
    ray = renderer.universeRenderer.ray;
    ray2 = renderer.universeRenderer.ray2;
    buffer = renderer.universeRenderer.buffer;
    screen = renderer.universeRenderer.screen;
    antialiasingFilter = renderer.universeRenderer.antialiasingFilter;
    colorMap = new Texture(Vars.tree.get("textures/color_map.png"), true);
    noise = new Texture(Vars.tree.get("textures/noise.png"), true);
    shader = new MenuBlackHoleShader();
    shader.galaxy = galaxy;
    shader.colorMap = colorMap;
    shader.noise = noise;
    shader.camera = cam;
    cam.position.set(10, 6, 10);
    cam.up.set(Vec3.Y.rotate(Vec3.X, 42f));
  }

  @Override
  public void render() {
    int w = Core.graphics.getWidth();
    int h = Core.graphics.getHeight();

    cam.resize(w, h);
    cam.position
        .setLength((radius + camRadius) * camLength + (zoom - 1f) * (radius + camRadius) * 2);
    cam.lookAt(Vec3.Zero);
    cam.update();
    if (rflag)
      ray2.begin();
    else
      ray.begin();
    shader.resolution = Tmp.v1.set(renderer.universeRenderer.rayW, renderer.universeRenderer.rayH);
    if (init) {
      if (rflag) {
        shader.previous = ray.getTexture();
      } else {
        shader.previous = ray2.getTexture();
      }
    } else {
      init = true;
      shader.previous = null;
    }
    shader.bind();
    shader.apply();
    screen.render(shader, Gl.triangles);
    if (rflag)
      ray2.end();
    else
      ray.end();
    rflag = !rflag;
    bloom.renderTo(ray, buffer);
    antialiasingFilter.setInput(buffer).render();
  }

  @Override
  public void dispose() {
    colorMap.dispose();
    shader.dispose();
  }

  @Override
  public void add(Group menu) {
    menu.fill(table -> {
      table.right().bottom();
      table.label(() -> "x:" + cam.position.x + " y:" + cam.position.y + " z:" + cam.position.z);
    });
    menu.touchable = Touchable.enabled;
    menu.dragged((cx, cy) -> {
      if (Core.input.getTouches() > 1)
        return;
      Vec3 pos = cam.position;

      float upV = pos.angle(Vec3.Y);
      float xscale = 9f, yscale = 10f;
      float margin = 1;
      float speed = 1f - Math.abs(upV - 90) / 90f;

      pos.rotate(cam.up, cx / xscale * speed);
      float amount = cy / yscale;
      amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

      pos.rotate(Tmp.v31.set(cam.up).rotate(cam.direction, 90), amount);
    });
    menu.addCaptureListener(new ElementGestureListener() {
      float lastZoom = -1f;

      @Override
      public void zoom(InputEvent event, float initialDistance, float distance) {
        if (lastZoom < 0) {
          lastZoom = zoom;
        }
        zoom = (Mathf.clamp(initialDistance / distance * lastZoom, minZoom, 2f));
      }

      @Override
      public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
        lastZoom = zoom;
      }
    });
  }
}
