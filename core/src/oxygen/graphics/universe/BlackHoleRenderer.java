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

import static oxygen.graphics.OCShaders.*;

public class BlackHoleRenderer implements Disposable {
  public final Cubemap galaxy = new Cubemap("cubemaps/stars/");
  public final Texture colorMap;
  public final Mesh screen;
  public FrameBuffer fboBlackhole, fboBrightness, fboBloomed;
  int width = Core.graphics.getWidth() ;
  int height = Core.graphics.getHeight() ;
  public static final int bloomIter = 8;
  public FrameBuffer[] fboDownsampled;
  public FrameBuffer[] fboUpsampled;
  public Mat view;
  public Vec3 pos = new Vec3(14f, 3f, 4f);
  public float roll = 36f * Mathf.degreesToRadians;

  public static int bloomIterations = 8;

  public BlackHoleRenderer() {
    screen = createScreenQuad();
    colorMap = new Texture(OCVars.getTexture("color_map.png"), true);
    Log.info("black hole init");
    colorMap.setFilter(TextureFilter.mipMap, TextureFilter.linear);
    colorMap.setWrap(TextureWrap.clampToEdge, TextureWrap.clampToEdge);
    fboBlackhole = new FrameBuffer(Format.rgba8888, width, height, true);
    fboBrightness = new FrameBuffer(Format.rgba8888, width, height, true);
    fboBloomed = new FrameBuffer(Format.rgba8888, width, height, true);
    fboDownsampled = new FrameBuffer[bloomIter];
    fboUpsampled = new FrameBuffer[bloomIter];
    for (int i = 0; i < bloomIter; i++) {
      fboDownsampled[i] =
          new FrameBuffer(Format.rgba8888, width >> (i + 1), height >> (i + 1), true);
      fboUpsampled[i] = new FrameBuffer(Format.rgba8888, width >> i, height >> i, true);
    }
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

  public Mesh createScreenQuad() {
    Mesh tmp = new Mesh(true, 4, 6, VertexAttribute.position);
    tmp.setVertices(new float[] { //
        -1f, -1f, //
        -1f, 1f, //
        1f, 1f, //
        1f, -1f,//
    });
    tmp.setIndices(new short[] {0, 1, 2, 0, 2, 3});
    return tmp;
  }

  void renderBlackHole() {
    Draw.flush();
    fboBlackhole.begin();
    Gl.clearColor(0f, 0f, 0f, 1);
    Gl.disable(Gl.depthTest);
    Gl.depthMask(false);
    Gl.clear(Gl.depthBufferBit);
    blackhole.pos = pos;
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

  void brightnessRender() {
    fboBrightness.begin();
    Gl.clearColor(0f, 0f, 0f, 1);
    Gl.disable(Gl.depthTest);
    Gl.clear(Gl.depthBufferBit);
    bloomBrightness.resolution = Tmp.v2.set(width, height);
    bloomBrightness.input = fboBlackhole.getTexture();
    bloomBrightness.bind();
    bloomBrightness.apply();
    screen.render(bloomBrightness, Gl.triangles);
    fboBrightness.end();
  }

  void bloom() {
    for (int level = 0; level < bloomIterations; level++) {
      if (level == 0)
        bloomDownsample.input = fboBrightness.getTexture();
      else
        bloomDownsample.input = fboDownsampled[level - 1].getTexture();
      FrameBuffer current = fboDownsampled[level];
      current.begin();
      Gl.clearColor(0f, 0f, 0f, 1);
      Gl.disable(Gl.depthTest);
      Gl.clear(Gl.depthBufferBit);
      bloomDownsample.resolution = Tmp.v2.set(width >> (level + 1), height >> (level + 1));
      bloomDownsample.bind();
      bloomDownsample.apply();
      screen.render(bloomDownsample, Gl.triangles);
      current.end();
    }
    for (int level = bloomIterations - 1; level >= 0; level--) {
      if (level == bloomIterations - 1)
        bloomUpsample.input = fboDownsampled[level].getTexture();
      else
        bloomUpsample.input = fboUpsampled[level + 1].getTexture();
      if (level == 0)
        bloomUpsample.addition = fboBrightness.getTexture();
      else
        bloomUpsample.addition = fboDownsampled[level - 1].getTexture();
      FrameBuffer current = fboUpsampled[level];
      current.begin();
      Gl.clearColor(0f, 0f, 0f, 1);
      Gl.disable(Gl.depthTest);
      Gl.clear(Gl.depthBufferBit);
      bloomUpsample.resolution = Tmp.v2.set(width >> level, height >> level);
      bloomUpsample.bind();
      bloomUpsample.apply();
      screen.render(bloomUpsample, Gl.triangles);
      current.end();
    }
    // composite
    fboBloomed.begin();
    Gl.clearColor(0f, 0f, 0f, 1);
    Gl.disable(Gl.depthTest);
    Gl.clear(Gl.depthBufferBit);
    bloomComposite.input = fboBlackhole.getTexture();
    bloomComposite.bloom = fboUpsampled[0].getTexture();
    bloomComposite.bind();
    bloomComposite.apply();
    screen.render(bloomComposite, Gl.triangles);
    fboBloomed.end();
  }

  void tonemappingRender() {
    tonemapping.input = fboBloomed.getTexture();
    tonemapping.bind();
    tonemapping.apply();
    screen.render(tonemapping, Gl.triangles);
  }

  public void render() {
    renderBlackHole();
    brightnessRender();
    bloom();
    tonemappingRender();
  }

  @Override
  public void dispose() {
    galaxy.dispose();
    colorMap.dispose();
    fboBlackhole.dispose();
    fboBrightness.dispose();
    fboBloomed.dispose();
    for (int i = 0; i < bloomIter; i++) {
      fboDownsampled[i].dispose();
      fboUpsampled[i].dispose();
    }

  }
}
