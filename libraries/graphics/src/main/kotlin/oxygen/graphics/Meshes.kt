package oxygen.graphics

import arc.*
import arc.graphics.*
import arc.graphics.gl.*
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

    fun solidCubeMesh(width: Float, height: Float, depth: Float) =
        begin(24, 36, uv = false, normal = true).apply {
            val halfW = width / 2f
            val halfH = height / 2f
            val halfD = depth / 2f
            verticesBuffer.put(
                floatArrayOf(
                    // 前平面 - 法线(0,0,1)
                    -halfW, -halfH, halfD, 0f, 0f, 1f,  // 0: 左下前
                    halfW, -halfH, halfD, 0f, 0f, 1f,  // 1: 右下前
                    halfW, halfH, halfD, 0f, 0f, 1f,  // 2: 右上前
                    -halfW, halfH, halfD, 0f, 0f, 1f,  // 3: 左上前

                    // 后平面 - 法线(0,0,-1)
                    halfW, -halfH, -halfD, 0f, 0f, -1f,  // 4: 右下后
                    -halfW, -halfH, -halfD, 0f, 0f, -1f,  // 5: 左下后
                    -halfW, halfH, -halfD, 0f, 0f, -1f,  // 6: 左上后
                    halfW, halfH, -halfD, 0f, 0f, -1f,  // 7: 右上后

                    // 右平面 - 法线(1,0,0)
                    halfW, -halfH, halfD, 1f, 0f, 0f,  // 8: 右前下
                    halfW, -halfH, -halfD, 1f, 0f, 0f,  // 9: 右后下
                    halfW, halfH, -halfD, 1f, 0f, 0f,  // 10: 右后上
                    halfW, halfH, halfD, 1f, 0f, 0f,  // 11: 右前上

                    // 左平面 - 法线(-1,0,0)
                    -halfW, -halfH, -halfD, -1f, 0f, 0f,  // 12: 左后下
                    -halfW, -halfH, halfD, -1f, 0f, 0f,  // 13: 左前下
                    -halfW, halfH, halfD, -1f, 0f, 0f,  // 14: 左前上
                    -halfW, halfH, -halfD, -1f, 0f, 0f,  // 15: 左后上

                    // 上平面 - 法线(0,1,0)
                    -halfW, halfH, halfD, 0f, 1f, 0f,  // 16: 左上前
                    halfW, halfH, halfD, 0f, 1f, 0f,  // 17: 右上前
                    halfW, halfH, -halfD, 0f, 1f, 0f,  // 18: 右上后
                    -halfW, halfH, -halfD, 0f, 1f, 0f,  // 19: 左上后

                    // 下平面 - 法线(0,-1,0)
                    -halfW, -halfH, -halfD, 0f, -1f, 0f,  // 20: 左下后
                    halfW, -halfH, -halfD, 0f, -1f, 0f,  // 21: 右下后
                    halfW, -halfH, halfD, 0f, -1f, 0f,  // 22: 右下前
                    -halfW, -halfH, halfD, 0f, -1f, 0f,  // 23: 左下前
                )
            )
            indicesBuffer.put(
                shortArrayOf(
                    0, 1, 2, 2, 3, 0,    // 前
                    4, 5, 6, 6, 7, 4,    // 后
                    8, 9, 10, 10, 11, 8, // 右
                    12, 13, 14, 14, 15, 12, // 左
                    16, 17, 18, 18, 19, 16, // 上
                    20, 21, 22, 22, 23, 20  // 下
                )
            )
        }.let(::end)

    private fun begin(vertices: Int, indices: Int, uv: Boolean, normal: Boolean) = Mesh(
        true, vertices, indices,
        *Seq.with(VertexAttribute.position3)
            .also { if (uv) it.add(VertexAttribute.texCoords) }
            .also { if (normal) it.add(VertexAttribute.normal) }
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

open class MeshPart(var id: String, var primitiveType: Int, var offset: Int, var size: Int, var mesh: Mesh) {
    fun set(other: MeshPart) = set(other.id, other.primitiveType, other.offset, other.size, other.mesh)
    fun set(id: String, primitiveType: Int, offset: Int, size: Int, mesh: Mesh): MeshPart {
        this.id = id
        this.primitiveType = primitiveType
        this.offset = offset
        this.size = size
        this.mesh = mesh
        return this
    }

    fun render(shader: Shader, autoBind: Boolean) {
        mesh.render(shader, primitiveType, offset, size, autoBind)
    }

    fun render(shader: Shader) {
        mesh.render(shader, primitiveType, offset, size)
    }
}

interface MeshPartBuilder {
    fun getMeshPart(): MeshPart
}
