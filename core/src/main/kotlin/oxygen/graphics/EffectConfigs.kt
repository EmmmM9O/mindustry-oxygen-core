package oxygen.graphics

import oxygen.*
import arc.graphics.*
import oxygen.graphics.bloom.*

object EffectConfigs {
    lateinit var renderer:ORenderer
    lateinit var bloom:OBloom
    fun init(){
        renderer = Oxygen.renderer
        bloom = renderer.obloom
    }
    fun default(){
        bloom.apply{
            threshold = 0.95f
            gamma = 0.6f
            intensity = 0.25f
            exposure = 0.8f
            scale = 1.6f
            mode = 4
        }
        renderer.apply {
            sclColor = Color(0.14f,0.13f,0.12f,0.1f)
        }
    }
}
