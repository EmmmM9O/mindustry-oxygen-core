package oxygen.graphics

import arc.graphics.*
import oxygen.*
import oxygen.graphics.bloom.*

object EffectConfigs {
    lateinit var renderer: ORenderer
    lateinit var bloom: OBloom
    fun init() {
        renderer = Oxygen.renderer
        bloom = renderer.obloom
    }

    fun default() {
        bloom.apply {
            threshold = 0.85f
            gamma = 0.65f
            intensity = 0.15f
            exposure = 0.8f
            scale = 1.6f
            mode = 4
        }
        renderer.apply {
            sclColor = Color(0.15f, 0.13f, 0.12f, 0.1f)
        }
    }
}
