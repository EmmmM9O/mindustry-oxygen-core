/* (C) 2025 */
package oxygen.graphics.gl;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.gl.*;

public class HDRFrameBuffer extends FrameBuffer {
  public HDRFrameBuffer() {

  }

  protected HDRFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
    super(bufferBuilder);
  }

  public HDRFrameBuffer(int width, int height, boolean hasDepth) {
    HDRFrameBufferBuilder bufferBuilder = new HDRFrameBufferBuilder(width, height);
    if (hasDepth)
      bufferBuilder.addBasicDepthRenderBuffer();
    bufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
    this.textureAttachments.clear();
    this.framebufferHandle = 0;
    this.depthbufferHandle = 0;
    this.stencilbufferHandle = 0;
    this.depthStencilPackedBufferHandle = 0;
    this.hasDepthStencilPackedBuffer = this.isMRT = false;
    this.bufferBuilder = bufferBuilder;
    build();
  }

  @Override
  protected Texture createTexture(FrameBufferTextureAttachmentSpec attachmentSpec) {
    Texture result = super.createTexture(attachmentSpec);
    if (Core.app.isDesktop())
      result.setFilter(TextureFilter.linear, TextureFilter.linear);
    else
      result.setFilter(TextureFilter.nearest, TextureFilter.nearest);
    return result;
  }

  @Override
  public void resize(int width, int height) {
    if (width == getWidth() && height == getHeight())
      return;

    TextureFilter min = getTexture().getMinFilter(), mag = getTexture().getMagFilter();
    boolean hasDepth = depthbufferHandle != 0, hasStencil = stencilbufferHandle != 0;
    dispose();

    HDRFrameBufferBuilder frameBufferBuilder = new HDRFrameBufferBuilder(width, height);
    frameBufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
    if (hasDepth)
      frameBufferBuilder.addBasicDepthRenderBuffer();
    if (hasStencil)
      frameBufferBuilder.addBasicStencilRenderBuffer();
    this.bufferBuilder = frameBufferBuilder;
    this.textureAttachments.clear();
    this.framebufferHandle = 0;
    this.depthbufferHandle = 0;
    this.stencilbufferHandle = 0;
    this.depthStencilPackedBufferHandle = 0;
    this.hasDepthStencilPackedBuffer = this.isMRT = false;
    build();
    getTexture().setFilter(min, mag);
  };

  public static class HDRFrameBufferBuilder extends GLFrameBufferBuilder<HDRFrameBuffer> {
    public HDRFrameBufferBuilder(int width, int height) {
      super(width, height);
    }

    @Override
    public HDRFrameBuffer build() {
      return new HDRFrameBuffer(this);
    }
  }
}
