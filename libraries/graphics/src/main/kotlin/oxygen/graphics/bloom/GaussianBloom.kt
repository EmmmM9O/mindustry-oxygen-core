package oxygen.graphics.bloom

import arc.*
import arc.util.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.graphics.Pixmap.*
import oxygen.graphics.*
import oxygen.graphics.gl.*

open class GaussianBloom(width: Int, height: Int, hasDepth:Boolean): CaptureBloom(){
    var blurPasses = 1
    lateinit var pingPong1: FrameBuffer
    lateinit var pingPong2: FrameBuffer

    lateinit var brightnessShader: OShader
    lateinit var blurShader: OShader
    lateinit var compositeShader: OShader
    init{
        init(width, height, hasDepth)
    }

    constructor(): this(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, true)
    constructor(hasDepth:Boolean): this(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4, hasDepth)

    override fun init(width: Int, height: Int, hasDepth:Boolean){
        super.init(width, height, hasDepth)
        pingPong1 = HDRFrameBuffer(width, height, false)
        pingPong2 = HDRFrameBuffer(width, height, false)

        brightnessShader = createShader("screenspace", "brightness")
        blurShader = createShader("blurspace", "gaussian")
        compositeShader = createShader("screenspace", "composite")
    }

    protected fun bindComposite(){
        compositeShader.bind()
        compositeShader.setUniformi("u_texture0", 0)
        compositeShader.setUniformi("u_texture1", 1)
        compositeShader.setUniformf("u_intensity", intensity)
        compositeShader.setUniformf("u_exposure", exposure)
        compositeShader.setUniformf("u_scale", scale)
        compositeShader.setUniformf("u_gamma", gamma)
        compositeShader.setUniformi("u_mode", mode)
    }

    protected fun blur(src: Texture){
        Gl.disable(Gl.depthTest)
        Gl.depthMask(false)
        blurShader.bind()
        blurShader.setUniformi("u_texture0", 0)
        blurShader.setUniformf("u_resolution", width.toFloat(), height.toFloat())

        pingPong1.begin()
        prework()
        brightnessShader.bind()
        brightnessShader.setUniformi("u_texture0", 0)
        brightnessShader.setUniformf("u_threshold", threshold)
        Draw.blit(src, brightnessShader)
        pingPong1.end()

        for(i in 0 until blurPasses){
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

    override fun render(texture:Texture){
        blur(texture)
        bindComposite()
        pingPong1.texture.bind(1)
        Draw.blit(texture, compositeShader)
        Gl.activeTexture(Gl.texture0)
    }

    override fun renderTo(src:FrameBuffer, dest:FrameBuffer){
        val texture = src.texture
        blur(texture)

        dest.begin()
        bindComposite()
        pingPong1.texture.bind(1)
        Draw.blit(texture, compositeShader)
        dest.end()
        Gl.activeTexture(Gl.texture0)
    }

    override fun onResize(){
        super.onResize()
        pingPong1.resize(width, height)
        pingPong2.resize(width, height)
    }

    override fun dispose(){
        super.dispose()
        pingPong1.dispose()
        pingPong2.dispose()
        brightnessShader.dispose()
        blurShader.dispose()
        compositeShader.dispose()
    }
}
