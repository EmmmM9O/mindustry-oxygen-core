/* (C) 2025 */
package oxygen.graphics.bloom;

import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.gl.*;

public abstract class CaptureBloom extends OCBloom {
  int width;
  int height;
  float r, g, b, a = 1.0f;
  public boolean capturing;
  public Mesh screen;
  FrameBuffer fboCapture;

  public CaptureBloom(Mesh screen) {
    this.screen = screen;
  }

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
    fboCapture.dispose();
  }

  @Override
  public void init(boolean hasDepth) {
    fboCapture = new FrameBuffer(Format.rgba8888, width, height, hasDepth);
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
  public void resize(int width, int height) {
    this.width = width;
    this.height = height;
    onResize();
  }

  @Override
  public void onResize() {
    fboCapture.resize(width, height);
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }
}
