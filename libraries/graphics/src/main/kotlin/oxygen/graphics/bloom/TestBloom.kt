package oxygen.graphics.bloom

import arc.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.math.*
import arc.util.*
import oxygen.graphics.*
import oxygen.graphics.gl.*

open class CompareBloom(width: Int, height: Int, hasDepth:Boolean): CaptureBloom() {
    var enableTest = false
    var choice = 3
    var blooms:MutableList<OBloom> = mutableListOf()
    lateinit var fbo: FrameBuffer

    override var threshold: Float =1.0f
        set(value) {
            field = value
            blooms.forEach { it.threshold = value }
        }

    override var gamma = 1.0f
    set(value) {
        field = value
        blooms.forEach { it.gamma = value }
    }
    override var exposure = 1.0f
    set(value) {
        field = value
        blooms.forEach { it.exposure = value }
    }
    override var intensity = 5.0f
    set(value) {
        field = value
        blooms.forEach { it.intensity = value }
    }
    override var scale = 1.0f
    set(value) {
        field = value
        blooms.forEach { it.scale = value }
    }
    override var mode = 0
    set(value) {
        field = value
        blooms.forEach { it.mode = value }
    }

    init{
        init(width, height, hasDepth)
    }

    constructor(): this(Core.graphics.getWidth(), Core.graphics.getHeight(), true)
    constructor(hasDepth:Boolean): this(Core.graphics.getWidth(), Core.graphics.getHeight(), hasDepth)

    override fun init(width: Int, height: Int, hasDepth:Boolean){
        super.init(width, height, hasDepth)
        fbo = HDRFrameBuffer(width, height, hasDepth)
    }
    val shader = OGShaders.screen
    fun render(texture:Texture,bloom:OBloom){
        fbo.begin()
        prework()
        bloom.render(texture)
        fbo.end()
        Draw.blit(fbo.texture,shader)
    }
    fun getMat(id:Int,mat:Mat):Mat = when(id){
        0 -> mat.translate(-1f,-1f).scale(0.5f,0.5f).translate(1f,1f)
        1 -> mat.translate(0f,-1f).scale(0.5f,0.5f).translate(1f,1f)
        2 -> mat.translate(-1f,0f).scale(0.5f,0.5f).translate(1f,1f)
        3 -> mat.translate(0f,0f).scale(0.5f,0.5f).translate(1f,1f)
        else -> mat
    }
    override fun render(texture:Texture){
        if(!enableTest){
            if(choice == 0){
                prework()
                shader.bind()
                shader.setUniformMatrix4("u_trans", Tmp.m3.idt())
                Draw.blit(texture,shader)
            }else{
                prework()
                blooms[choice - 1].render(texture)
            }
            return
        }
        prework()
        shader.bind()
        shader.setUniformMatrix4("u_trans", getMat(0,Tmp.m3.idt()))
        Draw.blit(texture,shader)
        blooms.withIndex().forEach{ (index, bloom) ->
            shader.bind() 
            shader.setUniformMatrix4("u_trans", getMat(index + 1,Tmp.m3.idt()))
            render(texture,bloom)
        }
    }
    override fun renderTo(src:FrameBuffer, dest:FrameBuffer){
        //Skip
    }
    override fun onResize(){
        super.onResize()
        blooms.forEach{ it.resize(width, height) }
    }

    override fun dispose(){
        super.dispose()
        blooms.forEach{ it.dispose() }
    }
}
