package oxygen.graphics.gl

import arc.graphics.*
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.gl.*

class HDRFrameBuffer : FrameBuffer {
    constructor()
    protected constructor(bufferBuilder: GLFrameBufferBuilder<out GLFrameBuffer<Texture>>) : super(bufferBuilder)
    constructor(width: Int, height: Int, hasDepth: Boolean = true) {
        val bufferBuilder = HDRFrameBufferBuilder(width, height)
        if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer()
        bufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, true)
        this.textureAttachments.clear()
        this.framebufferHandle = 0
        this.depthbufferHandle = 0
        this.stencilbufferHandle = 0
        this.depthStencilPackedBufferHandle = 0
        this.hasDepthStencilPackedBuffer = false
        this.isMRT = false
        this.bufferBuilder = bufferBuilder
        build()
    }

    override protected fun create(
        format: Pixmap.Format,
        width: Int,
        height: Int,
        hasDepth: Boolean,
        hasStencil: Boolean
    ) {
    }

    override protected fun createTexture(attachmentSpec: FrameBufferTextureAttachmentSpec): Texture {
        val data = GLOnlyTextureData(
            bufferBuilder.width,
            bufferBuilder.height,
            0,
            attachmentSpec.internalFormat,
            attachmentSpec.format,
            attachmentSpec.type
        )
        /*val data = FloatTextureData(bufferBuilder.width, bufferBuilder.height,
        attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.type,
        attachmentSpec.isGpuOnly)*/
        val result = Texture(data)
        /*if (Core.app.isDesktop())*/ result.setFilter(TextureFilter.linear, TextureFilter.linear)
        /*else result.setFilter(TextureFilter.nearest, TextureFilter.nearest)*/
        result.setWrap(TextureWrap.clampToEdge, TextureWrap.clampToEdge)
        return result
    }

    override fun resize(width: Int, height: Int) {
        if (width == getWidth() && height == getHeight()) return

        val min = getTexture().getMinFilter()
        val mag = getTexture().getMagFilter()
        val hasDepth = depthbufferHandle != 0
        val hasStencil = stencilbufferHandle != 0
        dispose()

        val frameBufferBuilder = HDRFrameBufferBuilder(width, height)
        frameBufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false)
        if (hasDepth) frameBufferBuilder.addBasicDepthRenderBuffer()
        if (hasStencil) frameBufferBuilder.addBasicStencilRenderBuffer()
        this.bufferBuilder = frameBufferBuilder
        this.textureAttachments.clear()
        this.framebufferHandle = 0
        this.depthbufferHandle = 0
        this.stencilbufferHandle = 0
        this.depthStencilPackedBufferHandle = 0
        this.hasDepthStencilPackedBuffer = false
        this.isMRT = false
        build()
        getTexture().setFilter(min, mag)
    }

    class HDRFrameBufferBuilder(width: Int, height: Int) : GLFrameBufferBuilder<HDRFrameBuffer>(width, height) {
        override fun build(): HDRFrameBuffer = HDRFrameBuffer(this)
    }
}
