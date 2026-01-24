package oxygen

import arc.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.math.geom.*
import oxygen.graphics.g2d.*

object OGraphics {
    lateinit var zbatch: ZBatch
    private var zTransformer: ((Float) -> Float)? = null
    private val retColor = Color()

    fun realZTransform(f: ((Float) -> Float)?) {
        zTransformer = f
    }

    fun realZ(): Float = zbatch.realZ

    fun realZ(z: Float) {
        if (zbatch != Core.batch) return
        zbatch.realZ(zTransformer?.invoke(z) ?: z)
    }

    fun draw(z: Float, run: () -> Unit) {
        if (zbatch != Core.batch) return
        realZ(z)
        Draw.draw(run)
    }

    fun trans3D(): Mat3D = zbatch.transform3DMatrix
    fun trans3D(mat: Mat3D) {
        if (zbatch != Core.batch) return
        zbatch.setTransform3DMatrix(mat)
    }

    fun proj3D(): Mat3D = zbatch.projection3DMatrix
    fun proj3D(mat: Mat3D) {
        if (zbatch != Core.batch) return
        zbatch.setProjection3DMatrix(mat)
    }

    fun vert(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if (zbatch != Core.batch) return
        zbatch.drawImpl(texture, spriteVertices, offset, count)
    }

    fun drawDepth(): Boolean = zbatch.depth
    fun drawDepth(value: Boolean) {
        zbatch.setDrawDepth(value)
    }

    fun getSclColor(): Color = retColor.abgr8888(zbatch.sclColorPacked)

    fun sclColor(color: Color) {
        zbatch.sclColorPacked = color.toFloatBits()
    }

    fun sclColor(r: Float, g: Float, b: Float, a: Float) {
        zbatch.sclColorPacked = Color.toFloatBits(r / COLOR_SCL, g / COLOR_SCL, b / COLOR_SCL, a / COLOR_SCL)
    }
}
