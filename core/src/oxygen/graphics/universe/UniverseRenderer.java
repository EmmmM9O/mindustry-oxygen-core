/* (C) 2025 */
package oxygen.graphics.universe;

import static oxygen.graphics.OCShaders.*;

import arc.*;
import arc.fx.*;
import arc.fx.filters.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.util.*;
import oxygen.graphics.*;
import oxygen.graphics.bloom.*;
import oxygen.util.*;

public class UniverseRenderer implements Disposable, Resizeable {
  public Cubemap galaxy;
  public OCBloom bloom;
  public final Camera3D cam = new Camera3D();
  public int width, height, rayW, rayH;
  public float blackholeScl = 2;
  public FrameBuffer buffer, ray, buffer2;
  public Mesh screen;
  public UniverseParams params;
  public FxFilter antialiasingFilter;

  public UniverseRenderer() {
    screen = OCMeshBuilder.screenMesh();
    cam.fov = 100f;
    cam.far = 20f;
    width = Core.graphics.getWidth();
    height = Core.graphics.getHeight();
    updateSize();
    galaxy = new Cubemap("cubemaps/stars/");
    bloom = new PyramidBloom(screen, width, height, false);
    buffer = new FrameBuffer(Format.rgba8888, width, height, true);
    buffer2 = new FrameBuffer(Format.rgba8888, width, height, true);
    ray = new FrameBuffer(Format.rgba8888, rayW, rayH, false);
    params = new UniverseParams();
    antialiasingFilter = new FxaaFilter();
    params.ray = ray;
    params.buffer = buffer;
    params.screen = screen;
    params.cam = cam;
  }

  public void updateSize() {
    rayW = (int) (width / blackholeScl);
    rayH = (int) (height / blackholeScl);
  }

  public void resizeRay() {
    updateSize();
    ray.resize(rayW, rayH);
  }

  public void resize() {
    resize(Core.graphics.getWidth(), Core.graphics.getHeight());
  }

  @Override
  public void resize(int width, int height) {
    this.width = width;
    this.height = height;
    onResize();
  }

  @Override
  public void onResize() {
    bloom.resize(width, height);
    buffer.resize(width, height);
    buffer2.resize(width, height);
    antialiasingFilter.resize(width, height);
    resizeRay();
  }

  @Override
  public void dispose() {
    blackhole.dispose();
    bloom.dispose();
    buffer.dispose();
    buffer2.dispose();
    ray.dispose();
  }

  public void render() {
    tonemapping.input = buffer.getTexture();
    tonemapping.bind();
    tonemapping.apply();
    screen.render(tonemapping, Gl.triangles);
  }

}
