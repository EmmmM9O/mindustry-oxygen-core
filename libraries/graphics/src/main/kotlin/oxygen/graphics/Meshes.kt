package oxygen.graphics

import arc.*
import arc.graphics.*
import arc.struct.*

object Meshes {
    private val gl30 = Core.gl30 != null

    /** Create a simple plane Mesh with uv */
    fun texturePlane(width: Float, height: Float) =
        begin(4, 6, uv = true, normal = false).apply {
            verticesBuffer.put(
                floatArrayOf(
                    -width / 2f, 0f, -height / 2f, 1f, 0f,
                    width / 2f, 0f, -height / 2f, 0f, 0f,
                    width / 2f, 0f, height / 2f, 0f, 1f,
                    -width / 2f, 0f, height / 2f, 1f, 1f
                )
            )
            indicesBuffer.put(shortArrayOf(0, 1, 2, 2, 3, 0))
        }.let(::end)

    private fun begin(vertices: Int, indices: Int, uv: Boolean, normal: Boolean) = Mesh(
        true, vertices, indices,
        *Seq.with(VertexAttribute.position3)
            .also { if (uv) it.add(VertexAttribute.texCoords) }
            .also { if (normal) it.add(if (gl30) VertexAttribute.packedNormal else VertexAttribute.normal) }
            .toArray()).apply {
        verticesBuffer.limit(verticesBuffer.capacity())
        verticesBuffer.position(0)
        if (indices > 0) {
            indicesBuffer.limit(indicesBuffer.capacity())
            indicesBuffer.position(0)
        }
    }

    private fun end(mesh: Mesh) = mesh.apply {
        verticesBuffer.limit(verticesBuffer.position())
        if (numIndices > 0) {
            indicesBuffer.limit(indicesBuffer.position())
        }
    }

    fun packNormals(x: Float, y: Float, z: Float): Float {
        val xs = if (x < -1f / 512f) 1 else 0
        val ys = if (y < -1f / 512f) 1 else 0
        val zs = if (z < -1f / 512f) 1 else 0

        val vi = zs shl 29 or
                ((z * 511 + (zs shl 9)).toInt() and 511) shl 20 or
                ys shl 19 or
                ((y * 511 + (ys shl 9)).toInt() and 511) shl 10 or
                xs shl 9 or
                ((x * 511 + (xs shl 9)).toInt() and 511)

        return Float.fromBits(vi)
    }
}
