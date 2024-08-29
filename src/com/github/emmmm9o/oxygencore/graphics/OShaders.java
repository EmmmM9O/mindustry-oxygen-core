package com.github.emmmm9o.oxygencore.graphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.Core;
import arc.files.Fi;
import arc.graphics.gl.Shader;
import arc.struct.ObjectMap;
import arc.util.ArcRuntimeException;
import arc.util.Log;

/**
 * OShaders
 */
public class OShaders {
  public static Shader icpT, icosphere;
  public static OShader solarShader, atmPlanetMesh, atmosphere;

  public static Fi file(String path) {
    return Manager.mod.root.child("shaders").child(path);
  }

  public static class LoadShader extends Shader {
    public LoadShader(String name) {
      super(file(name + ".vert"), file(name + ".frag"));
    }
  }

  public static class ShaderPack {
    public String content;
    public boolean gl2Only = false;

    public ShaderPack(String content, boolean gl2Only) {
      this.content = content;
      this.gl2Only = gl2Only;
    }
  }

  public static ObjectMap<String, ShaderPack> shaderPacks;

  public static class OShader extends Shader {
    public static Pattern importPattern = Pattern.compile("@import\\(\"(.*?)\"\\);");

    public OShader(String name) {
      this(file(name + ".vert"), file(name + ".frag"));
    }

    public OShader(Fi vertexShader, Fi fragmentShader) {
      super(vertexShader, fragmentShader);
    }

    public OShader(String vertexShader, String fragmentShader) {
      super(vertexShader, fragmentShader);
    }

    @Override
    protected String preprocess(String source, boolean fragment) {
      Matcher matcher = importPattern.matcher(source);

      StringBuffer result = new StringBuffer();

      while (matcher.find()) {
        String importContent = matcher.group(1).trim();
        if (!shaderPacks.containsKey(importContent)) {
          throw new ArcRuntimeException("Shader import unkown pack:" + importContent);
        }
        var content = shaderPacks.get(importContent);
        if (content.gl2Only && Core.gl30 != null)
          matcher.appendReplacement(result, "");
        else
          matcher.appendReplacement(result, content.content);
      }
      matcher.appendTail(result);
      return super.preprocess(result.toString(), fragment);
    }
  }

  public static void init() {
    shaderPacks = new ObjectMap<>();
    shaderPacks.put("transpose", new ShaderPack(file("transpose.glsl").readString(), true));
    shaderPacks.put("inverse", new ShaderPack(file("inverse.glsl").readString(), true));
    icpT = new LoadShader("icpplanet");
    icosphere = new LoadShader("icosphere");
    solarShader = new OShader("solar");
    atmPlanetMesh = new OShader("planet");
    atmosphere = new OShader("atmosphere");
  }

}
