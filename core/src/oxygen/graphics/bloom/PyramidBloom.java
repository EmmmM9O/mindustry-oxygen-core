/* (C) 2025 */
package oxygen.graphics.bloom;

import arc.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.gl.*;
import arc.util.*;
import oxygen.graphics.*;
import oxygen.graphics.OCShaders.*;

public class PyramidBloom extends OCBloom {
  public final int bloomIter = 9;
  public int bloomIterations = 9;
  public BloomBrightness bloomBrightness;
  public BloomComposite bloomComposite;
  public BloomUpsample bloomUpsample;
  public BloomDownsample bloomDownsample;
  public Mesh screen;
  FrameBuffer fboBrightness, fboCapture;
  FrameBuffer[] fboDownsampled;
  FrameBuffer[] fboUpsampled;
  int width;
  int height;
  private float r, g, b, a = 1.0f;

  public void setClearColor(float r, float g, float b, float a) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = a;
  }

  void prework() {
    Gl.clearColor(r, g, b, a);
    Gl.clear(Gl.colorBufferBit | Gl.depthBufferBit);
  }

  public boolean capturing;

  @Override
  public void capture() {
    if (!capturing) {
      capturing = true;
      fboCapture.begin();
      prework();
    }
  }

  @Override
  public void capturePause() {
    if (capturing) {
      capturing = false;
      fboCapture.end();
    }
  }

  @Override
  public void captureContinue() {
    if (!capturing) {
      capturing = true;
      fboCapture.begin();
    }
  }

  @Override
  public void dispose() {
    bloomComposite.dispose();
    bloomBrightness.dispose();
    bloomUpsample.dispose();
    bloomDownsample.dispose();
    fboBrightness.dispose();
    for (int i = 0; i < bloomIter; i++) {
      fboDownsampled[i].dispose();
      fboUpsampled[i].dispose();
    }
  }

  public PyramidBloom() {
    this(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, true);
  }

  public PyramidBloom(int width, int height) {
    this(width, height, true);
  }

  public PyramidBloom(int width, int height, boolean hasDepth) {
    this.width = width;
    this.height = height;
    init(hasDepth);
  }

  @Override
  public void init(boolean hasDepth) {
    screen = OCMeshBuilder.screenMesh();
    bloomBrightness = new BloomBrightness();
    bloomComposite = new BloomComposite();
    bloomUpsample = new BloomUpsample();
    bloomDownsample = new BloomDownsample();
    fboCapture = new FrameBuffer(Format.rgba8888, width, height, hasDepth);
    fboBrightness = new FrameBuffer(Format.rgba8888, width, height, false);
    fboDownsampled = new FrameBuffer[bloomIter];
    fboUpsampled = new FrameBuffer[bloomIter];
    for (int i = 0; i < bloomIter; i++) {
      fboDownsampled[i] = new FrameBuffer(Format.rgba8888, width >> (i + 1), height >> (i + 1), false);
      fboUpsampled[i] = new FrameBuffer(Format.rgba8888, width >> i, height >> i, false);
    }
  }

  void renderBloom(Texture input) {
    fboBrightness.begin();
    Gl.disable(Gl.depthTest);
    prework();
    bloomBrightness.resolution = Tmp.v2.set(width, height);
    bloomBrightness.input = input;
    bloomBrightness.bind();
    bloomBrightness.apply();
    screen.render(bloomBrightness, Gl.triangles);
    fboBrightness.end();
    for (int level = 0; level < bloomIterations; level++) {
      if (level == 0)
        bloomDownsample.input = fboBrightness.getTexture();
      else
        bloomDownsample.input = fboDownsampled[level - 1].getTexture();
      FrameBuffer current = fboDownsampled[level];
      current.begin();
      Gl.disable(Gl.depthTest);
      prework();
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
      Gl.disable(Gl.depthTest);
      prework();
      bloomUpsample.resolution = Tmp.v2.set(width >> level, height >> level);
      bloomUpsample.bind();
      bloomUpsample.apply();
      screen.render(bloomUpsample, Gl.triangles);
      current.end();
    }
  }

  void bloomComposite(Texture input) {
    Gl.disable(Gl.depthTest);
    prework();
    bloomComposite.input = input;
    bloomComposite.bloom = fboUpsampled[0].getTexture();
    bloomComposite.bind();
    bloomComposite.apply();
    screen.render(bloomComposite, Gl.triangles);
  }

  @Override
  public void render() {
    capturePause();
    render(fboCapture.getTexture());
  }

  @Override
  public void renderTo(FrameBuffer src) {
    capturePause();
    renderTo(fboCapture, src);
  }

  @Override
  public void render(Texture texture) {
    renderBloom(texture);
    bloomComposite(texture);
  }

  @Override
  public void renderTo(FrameBuffer src, FrameBuffer dest) {
    Texture texture = src.getTexture();
    renderBloom(texture);
    dest.begin();
    prework();
    bloomComposite(texture);
    dest.end();
  }

  @Override
  public void resize(int width, int height) {
    this.width = width;
    this.height = height;
    onResize();
  }

  @Override
  public void onResize() {
    fboBrightness.resize(width, height);
    for (int i = 0; i < bloomIter; i++) {
      fboDownsampled[i].resize(width, height);
      fboUpsampled[i].resize(width, height);
    }
  }
}
