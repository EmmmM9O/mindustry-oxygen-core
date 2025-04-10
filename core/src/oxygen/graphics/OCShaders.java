/* (C) 2025 */
package oxygen.graphics;

import java.util.regex.*;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import oxygen.graphics.menu.*;

public class OCShaders {
  public static BlackHoleShader blackhole;
  public static TonemappingShader tonemapping;

  public static void init() {
    blackhole = new BlackHoleShader();
    tonemapping = new TonemappingShader();
  }

  // universe
  public static class BlackHoleParams {
    public float maxLength = 40f, horizonRadius = 1f, adiskInnerRadius = 2.6f,
        adiskOuterRadius = 12f, adiskHeight = 0.55f, adiskDensityV = 2, adiskDensityH = 4,
        adiskNoiseScale = 0.8f, adiskSpeed = 0.2f, adiskLit = 0.25f, adiskParticle = 1.0f,
        maxScl = 1f, minScl = 1f, sclR = 6f, sclT = 0.5f, stepSize = 0.1f;
    public int adiskNoiseLOD = 5, maxSteps = 200;
    public Camera3D camera;

    public void apply(Shader shader) {
      shader.setUniformf("camera_pos", camera.position);
      shader.setUniformf("camera_up", camera.up);
      shader.setUniformf("camera_dir", camera.direction);
      shader.setUniformf("max_length_2", maxLength * maxLength);
      shader.setUniformf("horizon_radius_2", horizonRadius * horizonRadius);
      shader.setUniformf("adisk_height", adiskHeight);
      shader.setUniformf("adisk_inner_radius", adiskInnerRadius);
      shader.setUniformf("adisk_outer_radius", adiskOuterRadius);
      shader.setUniformf("adisk_density_v", adiskDensityV);
      shader.setUniformf("adisk_density_h", adiskDensityH);
      shader.setUniformf("adisk_lit", adiskLit);
      shader.setUniformf("adisk_speed", adiskSpeed);
      shader.setUniformf("adisk_particle", adiskParticle);
      shader.setUniformf("adisk_noise_scale", adiskNoiseScale);
      shader.setUniformf("adisk_noise_LOD", adiskNoiseLOD * 1.0f);
      shader.setUniformf("max_steps", maxSteps);
      shader.setUniformf("max_scl", maxScl);
      shader.setUniformf("min_scl", minScl);
      shader.setUniformf("scl_r", sclR);
      shader.setUniformf("scl_t", sclT);
      shader.setUniformf("step_size", stepSize);
      shader.setUniformf("fovScale", (float) Math.tan((camera.fov * (Math.PI / 180)) / 2.0));
    }
  }

  /**
   * To render Blackhole for {@link oxygen.universe.celestial.BlackholeType}
   */
  public static class BlackHoleShader extends OCLoadShader {
    public Texture colorMap, origin;
    public Vec2 resolution;
    public Camera3D camera;
    public BlackHoleParams params = new BlackHoleParams();

    public BlackHoleShader() {
      super("universe/blackhole_unv", "screen");
    }

    @Override
    public void apply() {
      params.camera = camera;
      params.apply(this);
      setUniformf("time", Time.globalTime / 10f);
      setUniformf("resolution", resolution);
      origin.bind(0);
      setUniformi("origin", 0);
      colorMap.bind(1);
      setUniformi("color_map", 1);
      Gl.activeTexture(Gl.texture0);
    }
  }

  /**
   * to render for {@link MenuBlackhole}
   */
  public static class MenuBlackHoleShader extends OCLoadShader {
    public Texture colorMap;
    public Cubemap galaxy;
    public Vec2 resolution;
    public Camera3D camera;
    public BlackHoleParams params = new BlackHoleParams();

    public MenuBlackHoleShader() {
      super("universe/blackhole_menu", "screen");
    }

    @Override
    public void apply() {
      params.camera = camera;
      params.apply(this);
      setUniformf("time", Time.globalTime / 10f);
      setUniformf("resolution", resolution);
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
    public float threshold = 0.8f, softEdgeRange = 0.2f;
    // CIE 1931
    public Vec3 luminanceVector = new Vec3(0.2126, 0.7152, 0.0722);
    /* new Vec3(0.2125, 0.7154, 0.0721); */

    public BloomBrightness() {
      super("bloom/bloom_brightness", "screen");
    }

    @Override
    public void apply() {
      setUniformf("threshold", threshold);
      setUniformf("softEdgeRange", softEdgeRange);
      setUniformf("luminanceVector", luminanceVector);
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

    Pattern pattern;

    public String processImport(String input) {
      if (pattern == null)
        pattern = Pattern.compile("@import\\(([^)]+)\\);");

      Matcher matcher = pattern.matcher(input);
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(sb, getShaderFi(matcher.group(1)).readString());
      }
      matcher.appendTail(sb);
      return sb.toString();
    }

    @Override
    protected String preprocess(String source, boolean fragment) {
      return super.preprocess(processImport(source), fragment);
    }
  }

  public static Fi getShaderFi(String file) {
    return Vars.tree.get("shaders/" + file);
  }
}
