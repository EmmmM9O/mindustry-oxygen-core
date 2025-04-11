/* (C) 2025 */
package oxygen.graphics.gl;

import java.nio.*;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.util.*;

public class FloatImage implements Disposable {
  public FloatFrameBuffer img;
  public int width, height;
  public FloatBuffer buffer;
  public float[][] values;
  public boolean readed;

  public FloatImage(int width, int height, boolean hasDepth) {
    this.width = width;
    this.height = height;
    img = new FloatFrameBuffer(width, height, hasDepth);
  }

  public void porcess(Shader shader, Mesh mesh) {
    img.begin();
    Gl.clear(Gl.colorBufferBit);
    shader.bind();
    shader.apply();
    mesh.render(shader, Gl.triangles);
    img.end();
    readed = false;
  }

  public FloatBuffer getBuffer() {
    if (readed)
      return buffer;
    buffer = Buffers.newFloatBuffer(width * height);
    Gl.readPixels(0, 0, width, height, GL30.GL_RED, GL30.GL_FLOAT, buffer);
    readed = true;
    return buffer;
  }

  public void read() {
    if (readed)
      return;
    values = new float[width][height];
    FloatBuffer buf = getBuffer();
    buf.rewind();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        values[x][y] = buf.get();
      }
    }
  }

  public float get(int x, int y) {
    read();
    return values[x][y];
  }

  public Texture getTexture() {
    return img.getTexture();
  }

  @Override
  public void dispose() {
    img.dispose();
  }
}
