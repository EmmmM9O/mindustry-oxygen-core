package oxygen.graphics.bloom

import arc.*
import arc.util.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.graphics.Pixmap.*
import oxygen.graphics.*
import oxygen.graphics.gl.*

open class KawaseBloom {
    //TODO
}

abstract class DualFilterBloom: TonemapBloom(){
    val mipLevels = 5

    lateinit var downsampleShader: OShader
    lateinit var upsampleShader: OShader

    lateinit var fboBrightness: HDRFrameBuffer
    lateinit var fboDownsampled: Array<FrameBuffer>
    lateinit var fboUpsampled: Array<FrameBuffer>
    override fun init(width: Int, height: Int, hasDepth:Boolean){
        super.init(width, height, hasDepth)
        fboBrightness = HDRFrameBuffer(width, height, false)
        fboDownsampled = Array(mipLevels) { HDRFrameBuffer(width shr (it+1), height shr (it+1), false) }
        fboUpsampled = Array(mipLevels) { HDRFrameBuffer(width shr it, height shr it, false) }
    }

    protected fun renderBloom(src: Texture){
        Gl.disable(Gl.depthTest)
        Gl.depthMask(false)

        fboBrightness.begin()
        prework()
        Draw.blit(src, brightnessShader)
        fboBrightness.end()

        for(level in 0 until mipLevels){
            val current = fboDownsampled[level]
            val input = if(level == 0) fboBrightness.texture else fboDownsampled[level-1].texture
            current.begin()
            prework()
            downsampleShader.bind()
            downsampleShader.setUniformi("u_texture0", 0)
            downsampleShader.setUniformf("u_resolution", input.width.toFloat(), input.height.toFloat())
            downsampleShader.setUniformf("u_iteration", (level - 1).toFloat())
            Draw.blit(input, downsampleShader)
            current.end()
        }

        for(level in mipLevels-1 downTo 0){
            val input = if(level == mipLevels-1) fboDownsampled[level].texture else fboUpsampled[level+1].texture
            val addition = when(level){
                mipLevels-1 -> fboDownsampled[level-1].texture
                mipLevels-2 -> fboDownsampled[level + 1].texture
                else -> fboUpsampled[level+2].texture
            }
            val current = fboUpsampled[level]
            current.begin()
            prework()
            upsampleShader.bind()
            upsampleShader.setUniformi("u_texture0", 0)
            upsampleShader.setUniformi("u_texture1", 1) 
            upsampleShader.setUniformf("u_resolution", input.width.toFloat(), input.height.toFloat())
            upsampleShader.setUniformf("u_iteration", (mipLevels-2-level).toFloat())
            addition.bind(1) 
            Draw.blit(input, upsampleShader)
            current.end()
            Gl.activeTexture(Gl.texture0)
        }
    }

    protected fun renderComposite(texture:Texture){
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
        upsampleShader.dispose()
        downsampleShader.dispose()

        fboBrightness.dispose()
        fboDownsampled.forEach { it.dispose() }
        fboUpsampled.forEach { it.dispose() }
    }
}

class StandardDualFilterBloom(width: Int, height: Int, hasDepth:Boolean): DualFilterBloom(){
    init{
        upsampleShader = createShader("screenspace", "dualFilterUpsample")
        downsampleShader = createShader("screenspace", "dualFilterDownsample")
        init(width, height, hasDepth)
    }

    constructor(): this(Core.graphics.getWidth(), Core.graphics.getHeight(), true)
    constructor(hasDepth:Boolean): this(Core.graphics.getWidth(), Core.graphics.getHeight(), hasDepth)
}

class EnchancedDualFilterBloom(width: Int, height: Int, hasDepth:Boolean): DualFilterBloom(){
    init{
        upsampleShader = createShader("screenspace", "enhancedDualFilterUpsample")
        downsampleShader = createShader("screenspace", "enhancedDualFilterDownsample")
        init(width, height, hasDepth)
    }

    constructor(): this(Core.graphics.getWidth(), Core.graphics.getHeight(), true)
    constructor(hasDepth:Boolean): this(Core.graphics.getWidth(), Core.graphics.getHeight(), hasDepth)
}

