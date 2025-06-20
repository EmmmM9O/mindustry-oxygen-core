/* (C) 2025 */
package oxygen.graphics.bloom;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import oxygen.graphics.OCShaders.*;
import oxygen.graphics.gl.*;

// reference:https://github.com/ebruneton/black_hole_shader
// bloom
public class PyramidFilterBloom extends CaptureBloom {
  // It would takes so long to generate this filter so
  // I directly use it instead of generating it
  public static final ObjectMap<Integer, Float[][]> BLOOM_FILTERS = new ObjectMap<>() {
    {
      put(600, new Float[][] {
          { 0.537425f, 0.0200663f, 0.00720805f, 0.00159719f, 0.000907315f, 0.000275641f },
          { 0.102792f, 0.0185013f, 0.00291111f, 0.000519003f, 0.000519003f, 0.000519003f },
          { 0.0704669f, 0.0181097f, 0.00232751f, 0.00232751f, 0.0015737f, 0.0015737f },
          { 0.0117432f, 0.0117432f, 0.00226476f, 0.00154524f, 0.00116041f, 0.00116041f },
          { 0.00746695f, 0.00746695f, 0.00171226f, 0.00104832f, 0.000766638f, 0.000766638f },
          { 0.00478257f, 0.00478257f, 0.00100513f, 0.000818812f, 0.000397319f, 0.000397319f },
          { 0.0037712f, 0.0037712f, 0.000490892f, 0.000490892f, 0.000490892f, 0.000490892f },
          { 0.00108603f, 0.00108603f, 0.000924505f, 0.000924505f, 0.000141375f, 0.0f },
          { 0.000604275f, 0.000604275f, 0.000604275f, 0.000604275f, 0.000604275f, 0.000604275f } });

      put(800, new Float[][] {
          { 0.368483f, 0.0216534f, 0.00816305f, 0.00188928f, 0.00108659f, 0.0003135f },
          { 0.136249f, 0.0234538f, 0.0044714f, 0.00035596f, 0.00035596f, 0.00035596f },
          { 0.115467f, 0.0273797f, 0.00361202f, 0.00361202f, 0.0024381f, 0.0024381f },
          { 0.0185586f, 0.0185586f, 0.00364918f, 0.00244913f, 0.00186549f, 0.00186549f },
          { 0.0120676f, 0.0120676f, 0.00279834f, 0.00169769f, 0.00125113f, 0.00125113f },
          { 0.00782081f, 0.00782081f, 0.00165563f, 0.00133947f, 0.000653398f, 0.000653398f },
          { 0.00620986f, 0.00620986f, 0.0008107f, 0.0008107f, 0.0008107f, 0.0008107f },
          { 0.0017856f, 0.0017856f, 0.00153169f, 0.00153169f, 0.000231589f, 0.0f },
          { 0.000999842f, 0.000999842f, 0.000999842f, 0.000999842f, 0.000999842f, 0.000999842f } });

      put(1000,
          new Float[][] {
              { 0.256172f, 0.0203539f, 0.00797156f, 0.00192098f, 0.00111651f, 0.000302982f },
              { 0.153181f, 0.0252457f, 0.0056879f, 5.24724E-5f, 5.24724E-5f, 5.24724E-5f },
              { 0.154089f, 0.0348566f, 0.00470551f, 0.00470551f, 0.00317819f, 0.00317819f },
              { 0.0246407f, 0.0246407f, 0.00494194f, 0.00326092f, 0.00251954f, 0.00251954f },
              { 0.0163845f, 0.0163845f, 0.00384115f, 0.00230972f, 0.00171517f, 0.00171517f },
              { 0.010743f, 0.010743f, 0.00229079f, 0.00184054f, 0.000902617f, 0.000902617f },
              { 0.00858938f, 0.00858938f, 0.00112463f, 0.00112463f, 0.00112463f, 0.00112463f },
              { 0.00246603f, 0.00246603f, 0.0021316f, 0.0021316f, 0.000318642f, 0.0f },
              { 0.00138965f, 0.00138965f, 0.00138965f, 0.00138965f, 0.00138965f, 0.00138965f } });

      put(1200,
          new Float[][] {
              { 0.183275f, 0.0181576f, 0.00737853f, 0.00184847f, 0.00110057f, 0.000302961f },
              { 0.155444f, 0.026573f, 0.00631122f, 0.0f, 0.0f, 0.0f },
              { 0.175386f, 0.0406837f, 0.00558637f, 0.00558637f, 0.00379344f, 0.00379344f },
              { 0.0298822f, 0.0298822f, 0.00611926f, 0.00396558f, 0.00310871f, 0.00310871f },
              { 0.0203221f, 0.0203221f, 0.00481554f, 0.00287054f, 0.00214781f, 0.00214781f },
              { 0.0134794f, 0.0134794f, 0.00289524f, 0.00230992f, 0.00113896f, 0.00113896f },
              { 0.0108519f, 0.0108519f, 0.00142504f, 0.00142504f, 0.00142504f, 0.00142504f },
              { 0.00311065f, 0.00311065f, 0.0027096f, 0.0027096f, 0.000400402f, 0.0f },
              { 0.00176416f, 0.00176416f, 0.00176416f, 0.00176416f, 0.00176416f, 0.00176416f } });

      put(1400,
          new Float[][] { { 0.13507f, 0.015829f, 0.00665188f, 0.0017314f, 0.00105678f, 0.000303019f },
              { 0.150222f, 0.0270593f, 0.00654101f, 0.0f, 0.0f, 0.0f },
              { 0.188342f, 0.0450054f, 0.00639373f, 0.0062499f, 0.00430739f, 0.00430739f },
              { 0.034393f, 0.034393f, 0.00718937f, 0.00457886f, 0.00363942f, 0.00363942f },
              { 0.0239205f, 0.0239205f, 0.00572745f, 0.00338534f, 0.00255211f, 0.00255211f },
              { 0.016048f, 0.016048f, 0.00347179f, 0.00275076f, 0.00136368f, 0.00136368f },
              { 0.013009f, 0.013009f, 0.00171332f, 0.00171332f, 0.00171332f, 0.00171332f },
              { 0.00372297f, 0.00372297f, 0.00326808f, 0.00326808f, 0.000477361f, 0.0f },
              { 0.00212503f, 0.00212503f, 0.00212503f, 0.00212503f, 0.00212503f, 0.00212503f } });

      put(1600,
          new Float[][] {
              { 0.102246f, 0.013671f, 0.00592138f, 0.00159768f, 0.00100127f, 0.000299669f },
              { 0.14157f, 0.026801f, 0.00657111f, 0.0f, 0.0f, 0.0f },
              { 0.19617f, 0.0482116f, 0.00708177f, 0.0067665f, 0.00473424f, 0.00473424f },
              { 0.0382986f, 0.0382986f, 0.00816852f, 0.00511466f, 0.00412131f, 0.00412131f },
              { 0.0272343f, 0.0272343f, 0.00658764f, 0.00386174f, 0.00293299f, 0.00293299f },
              { 0.0184784f, 0.0184784f, 0.00402643f, 0.00316811f, 0.00157908f, 0.00157908f },
              { 0.0150825f, 0.0150825f, 0.00199223f, 0.00199223f, 0.00199223f, 0.00199223f },
              { 0.00430923f, 0.00430923f, 0.00381212f, 0.00381212f, 0.00055036f, 0.0f },
              { 0.00247558f, 0.00247558f, 0.00247558f, 0.00247558f, 0.00247558f, 0.00247558f } });
    }
  };
  public int maxLevel = 9;
  public int numLevel;
  public Seq<FrameBuffer> filterFbos, mipmapFbos;
  public Seq<Vec3[]> bloomFilters;
  public FilterBloom bloomShader;
  public FilterBloomUpsample upsample;
  public FilterBloomDownsample downsample;
  public FilterBloomComposite composite;

  public PyramidFilterBloom(Mesh screen) {
    this(screen, Core.graphics.getWidth(), Core.graphics.getHeight());
  }

  public PyramidFilterBloom(Mesh screen, int width, int height) {
    super(screen);
    this.screen = screen;
    this.width = width;
    this.height = height;
    filterFbos = new Seq<>();
    mipmapFbos = new Seq<>();
    bloomFilters = new Seq<>();
    init(false);
  }

  public void resizePyramid() {
    filterFbos.clear();
    mipmapFbos.clear();
    int level = 0;
    int w = width;
    int h = height;
    while (h > 2 && level < maxLevel) {
      mipmapFbos.add(new HDRFrameBuffer(w + 2, h + 2, false));
      filterFbos.add(new HDRFrameBuffer(w, h, false));
      level += 1;
      w = (int) Math.ceil(w / 2);
      h = (int) Math.ceil(h / 2);
    }
    numLevel = level;
    Seq<Integer> keys = BLOOM_FILTERS.keys().toSeq();
    int nearestSizeIndex = 0;
    int nearestSize = keys.get(nearestSizeIndex);
    for (int i = 1; i < keys.size; i++) {
      int size = keys.get(i);
      if (Math.abs(size - height) < Math.abs(nearestSize - height)) {
        nearestSizeIndex = i;
        nearestSize = size;
      }
    }
    Float[][] filters = BLOOM_FILTERS.get(nearestSize);
    bloomFilters.clear();
    for (int i = 0; i < numLevel; ++i) {
      int id = 0;
      Vec3[] bloomFilter = new Vec3[25];
      FrameBuffer cur = mipmapFbos.get(i);
      int width = cur.getWidth();
      int height = cur.getHeight();
      for (int y = -2; y <= 2; ++y) {
        int iy = Math.abs(y);
        for (int x = -2; x <= 2; ++x) {
          int ix = Math.abs(x);
          int index = ix < iy ? (iy * (iy + 1)) / 2 + ix : (ix * (ix + 1)) / 2 + iy;
          float ww = filters[i][index];
          bloomFilter[id] = new Vec3(x / width, y / height, ww);
          id++;
        }
      }
      bloomFilters.add(bloomFilter);
    }
  }

  @Override
  public void onResize() {
    super.onResize();
    resizePyramid();
  }

  void renderBloom(Texture input) {
    for (int level = 0; level < numLevel; ++level) {
      if (level == 0)
        downsample.input = input;
      else
        downsample.input = mipmapFbos.get(level - 1).getTexture();
      var target = mipmapFbos.get(level);
      target.begin();
      Gl.disable(Gl.depthTest);
      prework();
      downsample.resolution = Tmp.v2.set(downsample.input.width, downsample.input.height);
      downsample.bind();
      downsample.apply();
      screen.render(downsample, Gl.triangles);
      target.end();
    }
    for (int level = 0; level < numLevel; ++level) {
      var ori = mipmapFbos.get(level);
      bloomShader.input = ori.getTexture();
      bloomShader.resolution = Tmp.v2.set(ori.getWidth(), ori.getHeight());
      bloomShader.sourceSamplesUvw = bloomFilters.get(level);
      var target = filterFbos.get(level);
      target.begin();
      Gl.disable(Gl.depthTest);
      prework();
      bloomShader.bind();
      bloomShader.apply();
      screen.render(bloomShader, Gl.triangles);
      target.end();
    }
    Gl.enable(Gl.blend);
    Gl.blendEquation(Gl.funcAdd);
    Gl.blendFunc(Gl.one, Gl.one);
    for (int level = this.numLevel - 2; level >= 0; --level) {
      var target = filterFbos.get(level);
      target.begin();
      upsample.input = filterFbos.get(level + 1).getTexture();
      upsample.resolution = Tmp.v2.set(upsample.input.width, upsample.input.height);
      Gl.disable(Gl.depthTest);
      prework();
      upsample.bind();
      upsample.apply();
      screen.render(upsample, Gl.triangles);
      target.end();
    }
    Gl.disable(Gl.blend);
  }

  @Override
  public void render(Texture texture) {
    renderBloom(texture);
  }

  @Override
  public void renderTo(FrameBuffer src, FrameBuffer dest) {
    Texture texture = src.getTexture();
    renderBloom(texture);
    dest.begin();
    dest.end();
  }

  @Override
  public void init(boolean hasDepth) {
    super.init(hasDepth);
    resizePyramid();
  }

}
