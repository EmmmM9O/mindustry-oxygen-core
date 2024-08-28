package com.github.emmmm9o.oxygencore.graphics;

import com.github.emmmm9o.oxygencore.core.Manager;

import arc.files.Fi;
import arc.graphics.gl.Shader;

/**
 * OShaders
 */
public class OShaders {
  public static Shader icpT;

  public static Fi file(String path) {
    return Manager.mod.root.child("shaders").child(path);
  }

  public static void init() {
    icpT = new Shader(file("icpplanet.vert"), file("icpplanet.frag"));
  }
}
