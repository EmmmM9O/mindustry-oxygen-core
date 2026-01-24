package oxygen.graphics.bloom

import arc.*
import arc.util.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.graphics.Pixmap.*
import oxygen.graphics.*
import oxygen.graphics.gl.*

abstract class PyramidBloom: CaptureBloom(){
    val maxBloomIter = 5
    var bloomIterations = 5
    lateinit var fboBrightness: HDRFrameBuffer
    lateinit var fboDownsampled: Array<HDRFrameBuffer>
    lateinit var fboUpsampled: Array<HDRFrameBuffer>
    lateinit var brightnessShader: Shader 
    lateinit var downsampleShader: Shader 
    lateinit var upsampleShader: Shader 
    lateinit var compositeShader: Shader 

    override fun init(width: Int, height: Int, hasDepth:Boolean){
        super.init(width, height, hasDepth)
        fboBrightness = HDRFrameBuffer(width, height, false)
        fboDownsampled = Array(maxBloomIter) { HDRFrameBuffer(width shr (it+1), height shr (it+1), false) }
        fboUpsampled = Array(maxBloomIter) { HDRFrameBuffer(width shr it, height shr it, false) }
    }

    protected fun renderBloom(src: Texture){
        Gl.disable(Gl.depthTest)
        Gl.depthMask(false)

        fboBrightness.begin()
        prework()
        brightnessShader.bind()
        brightnessShader.setUniformi("u_texture0", 0)
        brightnessShader.setUniformf("u_threshold", threshold)
        Draw.blit(src, brightnessShader)
        fboBrightness.end()

        for(level in 0 until bloomIterations){
            val input = if(level == 0) fboBrightness.texture else fboDownsampled[level-1].texture
            val current = fboDownsampled[level]
            current.begin()
            prework()
            downsampleShader.bind()
            downsampleShader.setUniformi("u_texture0", 0)
            downsampleShader.setUniformf("u_resolution", current.width.toFloat(), current.height.toFloat())
            Draw.blit(input, downsampleShader)
            current.end()
        }

        for(level in bloomIterations-1 downTo 0){
            val input = if(level == bloomIterations-1) fboDownsampled[level].texture else fboUpsampled[level+1].texture
            val addition = if(level == 0) fboBrightness.texture else fboDownsampled[level - 1].texture
            val current = fboUpsampled[level]
            current.begin()
            prework()
            upsampleShader.bind()
            upsampleShader.setUniformi("u_texture0", 0)
            upsampleShader.setUniformi("u_texture1", 1)
            upsampleShader.setUniformf("u_resolution", current.width.toFloat(), current.height.toFloat())
            addition.bind(1) 
            Draw.blit(input, upsampleShader)
            current.end()
            Gl.activeTexture(Gl.texture0)
        }
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
    
    protected fun renderComposite(texture:Texture){
        bindComposite()
        fboUpsampled[0].texture.bind(1)
        Draw.blit(texture, compositeShader)
        Gl.activeTexture(Gl.texture0)
    }

    override fun render(texture:Texture){
        renderBloom(texture)
        renderComposite(texture)
    }

    override fun renderTo(src:FrameBuffer, dest:FrameBuffer){
        val texture = src.texture
        renderBloom(texture)
        dest.begin()
        renderComposite(texture)
        dest.end()
    }

    override fun onResize(){
        super.onResize()
        fboBrightness.resize(width, height)
        fboDownsampled.withIndex().forEach{(k,v)->v.resize(width shr (k+1), height shr (k+1))}
        fboUpsampled.withIndex().forEach{(k,v)->v.resize(width shr k, height shr k)}
    }

    override fun dispose(){
        super.dispose()
        brightnessShader.dispose()
        upsampleShader.dispose()
        downsampleShader.dispose()
        compositeShader.dispose()

        fboBrightness.dispose()
        fboDownsampled.forEach { it.dispose() }
        fboUpsampled.forEach { it.dispose() }
    }
}

class PyramidFourNAvgBloom(width: Int, height: Int, hasDepth:Boolean) : PyramidBloom(){
    init{
        brightnessShader = createShader("screenspace", "brightness")
        compositeShader = createShader("screenspace", "composite")
        upsampleShader = createShader("fourNBlurspace", "fourNUpsample")
        downsampleShader = createShader("fourNBlurspace", "fourNDownsample")
        init(width, height, hasDepth)
    }

    constructor(): this(Core.graphics.getWidth(), Core.graphics.getHeight(), true)
    constructor(hasDepth:Boolean): this(Core.graphics.getWidth(), Core.graphics.getHeight(), hasDepth)
}
