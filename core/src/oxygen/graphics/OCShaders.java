/* (C) 2025 */
package oxygen.graphics;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;

public class OCShaders {
  public static BlackHoleShader blackhole;
  public static TonemappingShader tonemapping;
  public static BloomBrightness bloomBrightness;
  public static BloomComposite bloomComposite;
  public static BloomUpsample bloomUpsample;
  public static BloomDownsample bloomDownsample;

  public static void init() {
    blackhole = new BlackHoleShader();
    bloomBrightness = new BloomBrightness();
    bloomComposite = new BloomComposite();
    bloomUpsample = new BloomUpsample();
    bloomDownsample = new BloomDownsample();
    tonemapping = new TonemappingShader();
  }

  // universe
  public static class BlackHoleShader extends OCLoadShader {
    public Vec3 pos;
    public Mat view;
    public Texture colorMap;
    public Cubemap galaxy;
    public Vec2 resolution;
    public float maxLength = 25f, fovScale = 1f, horizonRadius = 1f, adiskInnerRadius = 2.6f,
        adiskOuterRadius = 12f, adiskHeight = 0.55f, adiskDensityV = 2.0f, adiskDensityH = 4.0f,
        adiskNoiseScale = 0.6f, adiskSpeed = 0.5f, adiskLit = 0.6f, adiskParticle = 1.0f,
        maxScl = 3f, minScl = 0.2f, sclR = 2f, sclT = 4f;
    public int adiskNoiseLOD = 4, maxSteps = 150;

    public BlackHoleShader() {
      super("universe/blackhole", "screen");
    }

    @Override
    public void apply() {
      setUniformf("time", Time.globalTime / 10f);
      setUniformf("camera_pos", pos);
      setUniformMatrix("view", view);
      setUniformf("resolution", resolution);
      setUniformf("max_length_2", maxLength * maxLength);
      setUniformf("fov_scale", fovScale);
      setUniformf("horizon_radius_2", horizonRadius * horizonRadius);
      setUniformf("adisk_height", adiskHeight);
      setUniformf("adisk_inner_radius", adiskInnerRadius);
      setUniformf("adisk_outer_radius", adiskOuterRadius);
      setUniformf("adisk_density_v", adiskDensityV);
      setUniformf("adisk_density_h", adiskDensityH);
      setUniformf("adisk_lit", adiskLit);
      setUniformf("adisk_speed", adiskSpeed);
      setUniformf("adisk_particle", adiskParticle);
      setUniformf("adisk_noise_scale", adiskNoiseScale);
      setUniformf("adisk_noise_LOD", adiskNoiseLOD * 1.0f);
      setUniformf("max_steps", maxSteps);
      setUniformf("max_scl", maxScl);
      setUniformf("min_scl", minScl);
      setUniformf("scl_r", sclR);
      setUniformf("scl_t", sclT);
      galaxy.bind(0);
      setUniformi("galaxy", 0);
      colorMap.bind(1);
      setUniformi("color_map", 1);
      Gl.activeTexture(Gl.texture0);
    }
  }

  // bloom
  public static class BloomBrightness extends OCLoadShader {
    public Texture input;
    public Vec2 resolution;

    public BloomBrightness() {
      super("bloom/bloom_brightness", "screen");
    }

    @Override
    public void apply() {
      setUniformf("resolution", resolution);
      input.bind(0);
      setUniformf("texture0", 0);
      Gl.activeTexture(Gl.texture0);
    }
  }

  public static class BloomComposite extends OCLoadShader {
    public Texture input, bloom;
    public float tone = 1f, bloom_strength = 0.2f;

    public BloomComposite() {
      super("bloom/bloom_composite", "screen");
    }

    @Override
    public void apply() {
      setUniformf("tone", tone);
      setUniformf("bloom_strength", bloom_strength);
      input.bind(0);
      setUniformi("texture0", 0);
      bloom.bind(1);
      setUniformi("texture1", 1);
      Gl.activeTexture(Gl.texture0);
    }
  }

  public static class BloomDownsample extends OCLoadShader {
    public Texture input;
    public Vec2 resolution;

    public BloomDownsample() {
      super("bloom/bloom_downsample", "screen");
    }

    @Override
    public void apply() {
      setUniformf("resolution", resolution);
      input.bind(0);
      setUniformi("texture0", 0);
      Gl.activeTexture(Gl.texture0);
    }
  }

  public static class BloomUpsample extends OCLoadShader {
    public Texture input, addition;
    public Vec2 resolution;

    public BloomUpsample() {
      super("bloom/bloom_upsample", "screen");
    }

    @Override
    public void apply() {
      setUniformf("resolution", resolution);
      input.bind(0);
      setUniformi("texture0", 0);
      addition.bind(1);
      setUniformi("texture1", 1);
      Gl.activeTexture(Gl.texture0);
    }
  }

  public static class TonemappingShader extends OCLoadShader {
    public boolean enabled = true;
    public float gamma = 2.8f;
    public Texture input;

    public TonemappingShader() {
      super("tonemapping", "screen");
    }

    @Override
    public void apply() {
      setUniformf("gamma", gamma);
      setUniformf("enabled", enabled ? 1.0f : 0.0f);
      input.bind(0);
      setUniformf("texture0", 0);
      Gl.activeTexture(Gl.texture0);
    }
  }

  public static class OCLoadShader extends Shader {
    public OCLoadShader(String frag, String vert) {
      super(getShaderFi(vert + ".vert"), getShaderFi(frag + ".frag"));
    }
  }

  public static Fi getShaderFi(String file) {
    return Vars.tree.get("shaders/" + file);
  }
}
