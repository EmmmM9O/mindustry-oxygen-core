/* (C) 2025 */
package oxygen.graphics.universe;

import arc.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import oxygen.core.*;
import oxygen.graphics.*;
import oxygen.graphics.bloom.*;
import oxygen.util.*;

import static oxygen.graphics.OCShaders.*;

public class BlackHoleRenderer implements Disposable, Resizeable {
  public final Cubemap galaxy = new Cubemap("cubemaps/stars/");
  public final Texture colorMap;
  public final Mesh screen;
  public FrameBuffer fboBlackhole, fboBloomed;
  public float scl = 3, len = 1.0f;

  int width = (int) (Core.graphics.getWidth() / scl);
  int height = (int) (Core.graphics.getHeight() / scl);

  public Mat view;
  public Vec3 pos = new Vec3(18f, 3f, 4f);
  public float roll = 36f * Mathf.degreesToRadians;
  public PyramidBloom bloom;

  public BlackHoleRenderer() {
    screen = OCMeshBuilder.screenMesh();
    colorMap = new Texture(OCVars.getTexture("color_map.png"), true);
    Log.info("black hole init");
    colorMap.setFilter(TextureFilter.mipMap, TextureFilter.linear);
    colorMap.setWrap(TextureWrap.clampToEdge, TextureWrap.clampToEdge);
    fboBlackhole = new FrameBuffer(Format.rgba8888, width, height, false);
    fboBloomed = new FrameBuffer(Format.rgba8888, width, height, false);
    bloom = new PyramidBloom(width, height, false);
    view = new Mat();
    Vec3 rr = new Vec3(Mathf.sin(roll), Mathf.cos(roll), 0.0);
    Vec3 ww = new Vec3(0, 0, 0).sub(pos).nor();
    Vec3 uu = ww.cpy().crs(rr).nor();
    Vec3 vv = uu.cpy().crs(ww).nor();
    view.val[Mat.M00] = uu.x;
    view.val[Mat.M01] = uu.y;
    view.val[Mat.M02] = uu.z;
    view.val[Mat.M10] = vv.x;
    view.val[Mat.M11] = vv.y;
    view.val[Mat.M12] = vv.z;
    view.val[Mat.M20] = ww.x;
    view.val[Mat.M21] = ww.y;
    view.val[Mat.M22] = ww.z;
  }

  public void resize() {
    resize(
        (int) (Core.graphics.getWidth() / scl),
        (int) (Core.graphics.getHeight() / scl));
  }

  @Override
  public void resize(int width, int height) {
    this.width = width;
    this.height = height;
    onResize();
  }

  @Override
  public void onResize() {
    fboBlackhole.resize(width, height);
    bloom.resize(width, height);
  }

  void renderBlackHole() {
    Draw.flush();
    fboBlackhole.begin();
    Gl.clearColor(0f, 0f, 0f, 1);
    Gl.disable(Gl.depthTest);
    Gl.depthMask(false);
    Gl.clear(Gl.depthBufferBit);
    blackhole.pos = Tmp.v31.set(pos).scl(len);
    blackhole.view = view;
    blackhole.galaxy = galaxy;
    blackhole.colorMap = colorMap;
    blackhole.resolution = Tmp.v2.set(width, height);
    blackhole.bind();
    blackhole.apply();
    screen.render(blackhole, Gl.triangles);

    Gl.depthMask(true);
    fboBlackhole.end();
  }

  void bloomRender() {
    bloom.renderTo(fboBlackhole, fboBloomed);
  }

  void tonemappingRender() {
    tonemapping.input = fboBloomed.getTexture();
    tonemapping.bind();
    tonemapping.apply();
    screen.render(tonemapping, Gl.triangles);
  }

  public void render() {
    renderBlackHole();
    bloomRender();
    tonemappingRender();
  }

  @Override
  public void dispose() {
    galaxy.dispose();
    colorMap.dispose();
    fboBlackhole.dispose();
    bloom.dispose();
  }
}
