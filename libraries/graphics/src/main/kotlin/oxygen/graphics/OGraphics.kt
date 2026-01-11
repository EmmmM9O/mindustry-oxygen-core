package oxygen

import arc.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.math.geom.*
import oxygen.graphics.g2d.*

object OGraphics {
    lateinit var zbatch: ZBatch
    private var zTransformer: ((Float) -> Float)? = null

    fun realZTransform(f: ((Float) -> Float)?) {
        zTransformer = f
    }

    fun realZ(): Float = zbatch.realZ

    fun realZ(z: Float) {
        if(zbatch != Core.batch) return
        zbatch.realZ(zTransformer?.invoke(z) ?: z)
    }

    fun draw(z: Float, run: () -> Unit) {
        if(zbatch != Core.batch) return
        realZ(z)
        Draw.draw(run)
    }

    fun trans3D(): Mat3D = zbatch.transform3DMatrix
    fun trans3D(mat: Mat3D) {
        if(zbatch != Core.batch) return
        zbatch.setTransform3DMatrix(mat)
    }

    fun proj3D(): Mat3D = zbatch.projection3DMatrix
    fun proj3D(mat: Mat3D) {
        if(zbatch != Core.batch) return
        zbatch.setProjection3DMatrix(mat)
    }

    fun vert(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if(zbatch != Core.batch) return
        zbatch.drawImpl(texture, spriteVertices, offset, count)
    }

    fun drawDepth() :Boolean = zbatch.depth
    fun drawDepth(value: Boolean) {
        zbatch.setDrawDepth(value)
    }

}
