package oxygen.graphics.bloom

import arc.util.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import oxygen.graphics.postprocessing.*
import oxygen.graphics.gl.*
import oxygen.graphics.*

abstract class OBloom: PostProcessorEffect(), Disposable{
    abstract fun capture()
    abstract fun capturePause()
    abstract fun captureContinue()

    abstract fun render()
    abstract fun renderTo(src: FrameBuffer)
    abstract fun render(texture: Texture)

    abstract fun init(width: Int, height: Int, hasDepth: Boolean)
    abstract fun resize(width: Int, height: Int)
    open fun onResize() {}

    abstract fun buffer(): FrameBuffer

    open var gamma = 1.0f
    open var exposure = 1.0f
    open var intensity = 5.0f
    open var threshold = 1.0f
    open var scale = 1.0f
    open var mode = 0

    var r = 1.0f
    var g = 1.0f
    var b = 1.0f
    var a = 1.0f
    fun setClearColor(r:Float, g:Float, b:Float, a:Float){
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun createShader(vert:String, frag:String):OShader = OShader("bloom/$vert","bloom/$frag").setup()
}

abstract class CaptureBloom: OBloom(){
    open var width = 0
    open var height = 0
    var capturing = false
    lateinit var fboCapture: FrameBuffer

    protected fun prework(){
        Gl.clearColor(r, g, b, a)
        Gl.clear(Gl.colorBufferBit or Gl.depthBufferBit)
    }

    override fun capture(){
        if(capturing) return
        capturing = true
        fboCapture.begin()
        prework() 
    }

    override fun capturePause(){
        if(!capturing) return
        capturing = false
        fboCapture.end()
    }

    override fun captureContinue(){
        if(capturing) return
        capturing = true
        fboCapture.begin()
    }

    override fun init(width: Int, height: Int, hasDepth: Boolean){
        fboCapture = HDRFrameBuffer(width, height, hasDepth)
        this.width = width
        this.height = height
    }

    override fun render(){
        capturePause()
        render(fboCapture.getTexture())
    }

    override fun renderTo(src: FrameBuffer){
        capturePause()
        renderTo(fboCapture, src)
    }

    override fun resize(width: Int, height: Int){
        if(this.width == width && this.height == height) return
        this.width = width
        this.height = height
        onResize()
    }

    override fun buffer(): FrameBuffer = fboCapture

    override fun onResize(){
        fboCapture.resize(width, height)
    }

    override fun dispose(){
        fboCapture.dispose()
    }
}

