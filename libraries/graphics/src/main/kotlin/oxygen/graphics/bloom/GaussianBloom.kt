package oxygen.graphics.bloom

import arc.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import oxygen.graphics.*
import oxygen.graphics.gl.*

open class GaussianBloom(width: Int, height: Int, hasDepth: Boolean) : TonemapBloom() {
    var blurPasses = 1
    lateinit var pingPong1: FrameBuffer
    lateinit var pingPong2: FrameBuffer

    lateinit var blurShader: OShader

    init {
        init(width, height, hasDepth)
    }

    constructor() : this(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, true)
    constructor(hasDepth: Boolean) : this(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, hasDepth)

    override fun init(width: Int, height: Int, hasDepth: Boolean) {
        super.init(width, height, hasDepth)
        pingPong1 = HDRFrameBuffer(width, height, false)
        pingPong2 = HDRFrameBuffer(width, height, false)

        blurShader = createShader("blurspace", "gaussian")
        resetBlurShader()
    }

    open fun resetBlurShader() {
        blurShader.bind()
        blurShader.setUniformi("u_texture0", 0)
        blurShader.setUniformf("u_resolution", width.toFloat(), height.toFloat())
    }

    protected fun blur(src: Texture) {
        Gl.disable(Gl.depthTest)
        Gl.depthMask(false)

        pingPong1.begin()
        prework()
        Draw.blit(src, brightnessShader)
        pingPong1.end()

        for (i in 0 until blurPasses) {
            pingPong2.begin()
            prework()
            blurShader.bind()
            blurShader.setUniformf("u_dir", 1f, 0f)
            pingPong1.blit(blurShader)
            pingPong2.end()

            pingPong1.begin()
            prework()
            blurShader.bind()
            blurShader.setUniformf("u_dir", 0f, 1f)
            pingPong2.blit(blurShader)
            pingPong1.end()
        }
    }

    override fun render(texture: Texture) {
        blur(texture)
        pingPong1.texture.bind(1)
        Draw.blit(texture, compositeShader)
        Gl.activeTexture(Gl.texture0)
    }

    override fun renderTo(src: FrameBuffer, dest: FrameBuffer) {
        val texture = src.texture
        blur(texture)

        dest.begin()
        pingPong1.texture.bind(1)
        Draw.blit(texture, compositeShader)
        dest.end()
        Gl.activeTexture(Gl.texture0)
    }

    override fun onResize() {
        super.onResize()
        pingPong1.resize(width, height)
        pingPong2.resize(width, height)
        blurShader.bind()
        blurShader.setUniformf("u_resolution", width.toFloat(), height.toFloat())
    }

    override fun resume() {
        super.resume()
        resetBlurShader()
    }

    override fun dispose() {
        super.dispose()
        pingPong1.dispose()
        pingPong2.dispose()
        blurShader.dispose()
    }
}
