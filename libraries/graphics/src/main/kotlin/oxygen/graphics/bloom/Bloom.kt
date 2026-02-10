package oxygen.graphics.bloom

import arc.graphics.*
import arc.graphics.gl.*
import arc.util.*
import oxygen.graphics.*
import oxygen.graphics.gl.*
import oxygen.graphics.postprocessing.*

abstract class OBloom : PostProcessorEffect(), Disposable {
    abstract fun capture()
    abstract fun capturePause()
    abstract fun captureContinue()

    abstract fun render()
    abstract fun renderTo(src: FrameBuffer)
    abstract fun render(texture: Texture)

    abstract fun init(width: Int, height: Int, hasDepth: Boolean)
    abstract fun resize(width: Int, height: Int)
    open fun onResize() {}

    open fun resume() {}

    abstract fun buffer(): FrameBuffer

    open var width = 0
    open var height = 0

    open var threshold = 1.0f

    open var gamma = 1.0f
    open var exposure = 1.0f
    open var intensity = 5.0f
    open var scale = 1.0f
    open var mode = 0

    var r = 1.0f
    var g = 1.0f
    var b = 1.0f
    var a = 1.0f
    fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun createShader(vert: String, frag: String): OShader = OShader("bloom/$vert", "bloom/$frag").setup()
    fun createShader(vert: String, frag: String, work: OShader.() -> Unit): OShader =
        OShader("bloom/$vert", "bloom/$frag").apply(work).setup()

    fun createShader(vert: String, frag: String, macros: Map<String, String>): OShader =
        createShader(vert, frag) { this.macros = macros }
}

abstract class CaptureBloom : OBloom() {
    var capturing = false
    lateinit var fboCapture: FrameBuffer

    protected fun prework() {
        Gl.clearColor(r, g, b, a)
        Gl.clear(Gl.colorBufferBit or Gl.depthBufferBit)
    }

    override fun capture() {
        if (capturing) return
        capturing = true
        fboCapture.begin()
        prework()
    }

    override fun capturePause() {
        if (!capturing) return
        capturing = false
        fboCapture.end()
    }

    override fun captureContinue() {
        if (capturing) return
        capturing = true
        fboCapture.begin()
    }

    override fun init(width: Int, height: Int, hasDepth: Boolean) {
        fboCapture = HDRFrameBuffer(width, height, hasDepth)
        this.width = width
        this.height = height
    }

    override fun render() {
        capturePause()
        render(fboCapture.getTexture())
    }

    override fun renderTo(src: FrameBuffer) {
        capturePause()
        renderTo(fboCapture, src)
    }

    override fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) return
        this.width = width
        this.height = height
        onResize()
    }

    override fun buffer(): FrameBuffer = fboCapture

    override fun onResize() {
        fboCapture.resize(width, height)
    }

    override fun dispose() {
        fboCapture.dispose()
    }
}

abstract class TonemapBloom : CaptureBloom() {
    lateinit var brightnessShader: OShader
    lateinit var compositeShader: OShader

    override var threshold: Float = 1.0f
        set(value) {
            field = value
            brightnessShader.bind()
            brightnessShader.setUniformf("u_threshold", value)
        }

    override var exposure: Float = 1.0f
        set(value) {
            field = value
            compositeShader.bind()
            compositeShader.setUniformf("u_exposure", value)
        }
    override var gamma: Float = 1.0f
        set(value) {
            field = value
            compositeShader.bind()
            compositeShader.setUniformf("u_gamma", value)
        }
    override var intensity: Float = 5.0f
        set(value) {
            field = value
            compositeShader.bind()
            compositeShader.setUniformf("u_intensity", value)
        }
    override var scale: Float = 1.0f
        set(value) {
            field = value
            compositeShader.bind()
            compositeShader.setUniformf("u_scale", value)
        }
    override var mode: Int = 0
        set(value) {
            field = value
            reloadComposite()
        }

    open fun resetBrightnessShader() {
        brightnessShader.bind()
        brightnessShader.setUniformi("u_texture0", 0)
        brightnessShader.setUniformf("u_threshold", threshold)
    }

    open fun reloadComposite() {
        if (this::compositeShader.isInitialized) compositeShader.dispose()
        compositeShader = createShader("screenspace", "composite", mapOf(Macros.tonemapMode to "$mode"))
        resetCompositeShader()
    }

    open fun resetCompositeShader() {
        compositeShader.bind()
        compositeShader.setUniformi("u_texture0", 0)
        compositeShader.setUniformi("u_texture1", 1)
        compositeShader.setUniformf("u_intensity", intensity)
        compositeShader.setUniformf("u_exposure", exposure)
        compositeShader.setUniformf("u_scale", scale)
        compositeShader.setUniformf("u_gamma", gamma)
    }

    override fun resume() {
        resetBrightnessShader()
        resetCompositeShader()
    }

    override fun init(width: Int, height: Int, hasDepth: Boolean) {
        super.init(width, height, hasDepth)
        brightnessShader = createShader("screenspace", "brightness")
        resetBrightnessShader()
        mode = this.mode
    }

    override fun dispose() {
        super.dispose()
        brightnessShader.dispose()
        compositeShader.dispose()
    }
}
