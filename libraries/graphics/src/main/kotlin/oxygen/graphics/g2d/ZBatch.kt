package oxygen.graphics.g2d

import arc.*
import arc.func.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import oxygen.graphics.*
import oxygen.math.*
import java.nio.*
import java.util.*
import java.util.concurrent.*
import kotlin.math.*

const val COLOR_SCL = 10f

abstract class ZBatch : Batch() {
    val transform3DMatrix = Mat3D()
    val combinedTrans = Mat3D()
    val projection3DMatrix = Mat3D()
    val tmpMat = Mat3D()
    var alphaTest = 0f
    var sortRealZ = true
    var depth = false
    var depthShader: Shader? = null
    var customDepthShader: Shader? = null

    var sclColorPacked = Color(1f / COLOR_SCL, 1f / COLOR_SCL, 1f / COLOR_SCL, 1f / COLOR_SCL).toFloatBits()
    var beforeVert: (ZBatch.() -> Unit)? = null
    var afterVert: (ZBatch.() -> Unit)? = null

    fun setProjection3DMatrix(matrix: Mat3D) {
        flush()
        projection3DMatrix.set(matrix)
    }

    fun setTransform3DMatrix(matrix: Mat3D) {
        flush()
        transform3DMatrix.set(matrix)
    }

    var realZ: Float = 0f
    open fun realZ(z: Float) {

    }

    fun setDrawDepth(draw: Boolean) {
        if (depth == draw) return
        flush()
        depth = draw
    }

    fun getDrawZ(): Float = this.realZ + this.z / 300f

    fun getSortZ(): Float = if (sortRealZ) getDrawZ() else this.z

    override fun setupMatrices() {
        combinedTrans.set(transform3DMatrix)
            .mul(combinedMatrix.set(projectionMatrix).mul(transformMatrix).to3D(tmpMat))
        val sha = getShader()
        sha.setUniformMatrix4("u_trans", combinedTrans.`val`)
        sha.setUniformMatrix4("u_proj", projection3DMatrix.`val`)
    }

    open fun setDepth(shader: Shader?) {
        flush()
        this.customDepthShader = shader
    }

    override fun getShader(): Shader = if (depth) customDepthShader ?: depthShader!! else super.getShader()

    abstract fun drawImpl(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int)
}


/**
 *
 * Class for efficiently batching and sorting sprites.
 *
 * Sorting optimizations written by zxtej.
 * Significant request optimizations done by way-zer.
 *
 * Y Support
 */
class ZSpriteBatch(size: Int = 4096, defaultShader: Shader? = null, defaultDepth: Shader? = null) : ZBatch() {
    var multithreaded: Boolean =
        Core.app != null && ((Core.app.version >= 21 && !Core.app.isIOS) || Core.app.isDesktop)

    private val mesh: Mesh
    private val buffer: FloatBuffer

    val tmpVertices: FloatArray = FloatArray(SPRITE_SIZE)

    var requestVerts: FloatArray = FloatArray(INITIAL_SIZE * SPRITE_SIZE)
    var requestVertOffset: Int = 0

    private var sort: Boolean = false
    private var flushing: Boolean = false
    private var requests: Array<DrawRequest?> = arrayOfNulls<DrawRequest>(INITIAL_SIZE)
    private var copy: Array<DrawRequest?> = arrayOfNulls<DrawRequest>(0)
    private var requestZ: IntArray = IntArray(INITIAL_SIZE)
    private var numRequests: Int = 0
    private var contiguous: IntArray = IntArray(2048)
    private var contiguousCopy: IntArray = IntArray(2048)
    private var intZ: Int = java.lang.Float.floatToRawIntBits(z + 16f)

    class DrawRequest {
        var verticesOffset: Int = 0
        var verticesLength: Int = 0
        var texture: Texture? = null
        var blending: Blending? = null
        var run: Runnable? = null
        var realZ: Float = 0f
    }

    /**
     * Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
     * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
     * respect to the current screen resolution.
     *
     *
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with [.setShader].
     * @param size The max number of sprites in a single batch. Max of 8191.
     * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately.
     */
    /**
     * Constructs a new SpriteBatch with a size of 4096, one buffer, and the default shader.
     * @see .SpriteBatch
     */
    /**
     * Constructs a SpriteBatch with one buffer and the default shader.
     * @see .SpriteBatch
     */
    init {
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        require(size <= 8191) { "Can't have more than 8191 sprites per batch: $size" }

        require(size > 0) { "size must > 0 $size" }

        projectionMatrix.setOrtho(0f, 0f, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())

        mesh = Mesh(
            true, false, size * 4, size * 6,
            VertexAttribute.position3,
            VertexAttribute.color,
            VertexAttribute.texCoords,
            VertexAttribute.mixColor,
            VertexAttribute(4, GL20.GL_UNSIGNED_BYTE, true, "a_scl_color")
        )

        val len = size * 6
        val indices = ShortArray(len)
        var j: Short = 0
        var i = 0
        while (i < len) {
            indices[i] = j
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j
            i += 6
            j = (j + 4).toShort()
        }
        mesh.setIndices(indices)
        mesh.verticesBuffer.position(0)
        mesh.verticesBuffer.limit(mesh.verticesBuffer.capacity())

        if (defaultShader == null) {
            shader = createShader()
            ownsShader = true
        } else {
            shader = defaultShader
        }

        if (defaultDepth == null) {
            depthShader = createDepthShader()
        } else {
            depthShader = defaultDepth
        }

        //mark indices as dirty once for GL30
        mesh.indicesBuffer
        buffer = mesh.verticesBuffer

        for (i in requests.indices) {
            requests[i] = DrawRequest()
        }

        if (multithreaded) {
            try {
                commonPool = ForkJoinHolder()
            } catch (t: Throwable) {
                multithreaded = false
            }
        }
    }

    override fun dispose() {
        super.dispose()
        mesh.dispose()
    }

    override fun setSort(sort: Boolean) {
        if (this.sort != sort) {
            flush()
        }
        this.sort = sort
    }

    override fun setShader(shader: Shader?, apply: Boolean) {
        require(!(!flushing && sort)) { "Shaders cannot be set while sorting is enabled. Set shaders inside Draw.run(...)." }
        super.setShader(shader, apply)
    }

    override fun setBlending(blending: Blending) {
        this.blending = blending
    }

    override fun z(z: Float) {
        if (z == this.z) return
        this.z = z
        intZ = java.lang.Float.floatToRawIntBits(getSortZ() + 16f)
    }

    override fun realZ(z: Float) {
        if (z == this.realZ) return
        this.realZ = z
        intZ = java.lang.Float.floatToRawIntBits(getSortZ() + 16f)
    }

    override fun discard() {
        super.discard()

        buffer.position(0)
    }

    fun addRealZ(arrO: FloatArray, offset: Int, count: Int, z: Float): FloatArray {
        val original = arrO.copyOfRange(offset, offset + count)
        val chunkSize = 6
        val chunks = original.size / chunkSize
        val newSize = original.size + chunks * 2
        val newArray = FloatArray(newSize)

        beforeVert?.invoke(this)

        var srcIndex = 0
        var dstIndex = 0
        val sclColor = sclColorPacked

        for (chunk in 0 until chunks) {
            for (i in 0 until 2) {
                newArray[dstIndex++] = original[srcIndex++]
            }
            newArray[dstIndex++] = z
            for (i in 0 until 4) {
                newArray[dstIndex++] = original[srcIndex++]
            }
            newArray[dstIndex++] = sclColor
        }
        afterVert?.invoke(this)
        return newArray
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        drawImpl(texture, addRealZ(spriteVertices, offset, count, getDrawZ()), 0, count * 8 / 6)
    }

    override fun drawImpl(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if (sort && !flushing) {
            val num = numRequests
            if (num > 0) {
                val last = requests[num - 1]!!
                if (last.run == null && last.texture === texture && last.blending === blending && requestZ[num - 1] == intZ) {
                    if (spriteVertices != emptyVertices) {
                        prepare(count)
                        System.arraycopy(spriteVertices, offset, requestVerts, requestVertOffset, count)
                        requestVertOffset += count
                    }
                    last.verticesLength += count
                    return
                }
            }
            if (num >= this.requests.size) expandRequests()
            val req = requests[num]!!
            if (spriteVertices != emptyVertices) {
                req.verticesOffset = requestVertOffset
                prepare(count)
                System.arraycopy(spriteVertices, offset, requestVerts, requestVertOffset, count)
                requestVertOffset += count
            } else {
                req.verticesOffset = offset
            }
            req.verticesLength = count
            requestZ[num] = intZ
            req.texture = texture
            req.blending = blending
            req.run = null
            req.realZ = realZ
            numRequests++
        } else {
            drawSuper(texture, spriteVertices, offset, count)
        }
    }

    override fun draw(
        region: TextureRegion,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        rotation: Float
    ) {
        if (!sort || flushing) {
            drawSuper(region, x, y, originX, originY, width, height, rotation)
            return
        }
        val pos = this.requestVertOffset
        this.requestVertOffset += SPRITE_SIZE
        prepare(SPRITE_SIZE)
        constructVertices(this.requestVerts, pos, region, x, y, getDrawZ(), originX, originY, width, height, rotation)
        drawImpl(region.texture, emptyVertices, pos, SPRITE_SIZE)
    }

    override fun draw(request: Runnable) {
        if (sort && !flushing) {
            if (numRequests >= requests.size) expandRequests()
            val req = requests[numRequests]!!
            req.realZ = realZ
            req.run = request
            req.blending = blending
            requestZ[numRequests] = intZ
            req.texture = null
            numRequests++
        } else {
            request.run()
        }
    }

    private fun prepare(i: Int) {
        if (requestVertOffset + i >= requestVerts.size) requestVerts = requestVerts.copyOf(requestVerts.size shl 1)
    }

    private fun expandRequests() {
        val requests = this.requests
        val newRequests: Array<DrawRequest?> = requests.copyOf(requests.size * 7 / 4)
        for (i in requests.size..<newRequests.size) {
            newRequests[i] = DrawRequest()
        }
        this.requests = newRequests
        this.requestZ = requestZ.copyOf(newRequests.size)
    }

    override fun flush() {
        if (!flushing) {
            flushing = true
            flushRequests()
            flushing = false
        }

        if (idx == 0) return

        val sha = getShader()
        sha.bind()
        setupMatrices()
        sha.setUniformf("u_alphaTest", alphaTest)

        if (customShader != null && apply && !depth) {
            customShader.apply()
        }

        val count = idx / SPRITE_SIZE * 6

        blending.apply()

        lastTexture.bind()
        val mesh = this.mesh
        //calling buffer() marks it as dirty, so it gets reuploaded upon render
        mesh.verticesBuffer

        buffer.position(0)
        buffer.limit(idx)

        mesh.render(sha, Gl.triangles, 0, count)

        buffer.limit(buffer.capacity())
        buffer.position(0)

        idx = 0
    }

    private fun flushRequests() {
        if (numRequests == 0) return
        sortRequests()
        val preColor = colorPacked
        val preMixColor = mixColorPacked
        val preSclColor = sclColorPacked
        val preBlending = blending
        val prez = realZ

        val vertices: FloatArray = this.requestVerts
        val r = copy
        val num = numRequests
        for (j in 0..<num) {
            val req = r[j]!!

            super.setBlending(req.blending)
            realZ(req.realZ)

            if (req.run != null) {
                req.run!!.run()
                req.run = null
            } else if (req.texture != null) {
                drawSuper(req.texture!!, vertices, req.verticesOffset, req.verticesLength)
            } // the request is invalid, but crashing wouldn't be very nice, so it is simply ignored
        }

        realZ(prez)
        colorPacked = preColor
        mixColorPacked = preMixColor
        sclColorPacked = preSclColor
        blending = preBlending

        numRequests = 0
        requestVertOffset = 0
    }

    private fun drawSuper(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        var offset = offset
        var count = count
        val verticesLength = buffer.capacity()
        var remainingVertices = verticesLength
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else {
            remainingVertices -= idx
            if (remainingVertices == 0) {
                flush()
                remainingVertices = verticesLength
            }
        }
        var copyCount = min(remainingVertices, count)

        buffer.put(spriteVertices, offset, copyCount)

        idx += copyCount
        count -= copyCount
        while (count > 0) {
            offset += copyCount
            flush()
            copyCount = min(verticesLength, count)
            buffer.put(spriteVertices, offset, copyCount)
            idx += copyCount
            count -= copyCount
        }
    }

    private fun drawSuper(
        region: TextureRegion,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        rotation: Float
    ) {
        val texture = region.texture
        if (texture !== lastTexture) {
            switchTexture(texture)
        } else if (idx == buffer.capacity()) {
            flush()
        }

        this.idx += SPRITE_SIZE
        constructVertices(this.tmpVertices, 0, region, x, y, getDrawZ(), originX, originY, width, height, rotation)
        buffer.put(tmpVertices)
    }

    private fun constructVertices(
        vertices: FloatArray,
        idx: Int,
        region: TextureRegion,
        x: Float,
        y: Float,
        z: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        rotation: Float
    ) {
        val u = region.u
        val v = region.v2
        val u2 = region.u2
        val v2 = region.v
        beforeVert?.invoke(this)

        val color = this.colorPacked
        val mixColor = this.mixColorPacked
        val sclColor = this.sclColorPacked

        if (!Mathf.zero(rotation)) {
            //bottom left and top right corner points relative to origin
            val worldOriginX = x + originX
            val worldOriginY = y + originY
            val fx = -originX
            val fy = -originY
            val fx2 = width - originX
            val fy2 = height - originY

            // rotate
            val cos = Mathf.cosDeg(rotation)
            val sin = Mathf.sinDeg(rotation)

            val x1 = cos * fx - sin * fy + worldOriginX
            val y1 = sin * fx + cos * fy + worldOriginY
            val x2 = cos * fx - sin * fy2 + worldOriginX
            val y2 = sin * fx + cos * fy2 + worldOriginY
            val x3 = cos * fx2 - sin * fy2 + worldOriginX
            val y3 = sin * fx2 + cos * fy2 + worldOriginY
            val x4 = x1 + (x3 - x2)
            val y4 = y3 - (y2 - y1)

            vertices[idx] = x1
            vertices[idx + 1] = y1
            vertices[idx + 2] = z
            vertices[idx + 3] = color
            vertices[idx + 4] = u
            vertices[idx + 5] = v
            vertices[idx + 6] = mixColor
            vertices[idx + 7] = sclColor

            vertices[idx + 8] = x2
            vertices[idx + 9] = y2
            vertices[idx + 10] = z
            vertices[idx + 11] = color
            vertices[idx + 12] = u
            vertices[idx + 13] = v2
            vertices[idx + 14] = mixColor
            vertices[idx + 15] = sclColor

            vertices[idx + 16] = x3
            vertices[idx + 17] = y3
            vertices[idx + 18] = z
            vertices[idx + 19] = color
            vertices[idx + 20] = u2
            vertices[idx + 21] = v2
            vertices[idx + 22] = mixColor
            vertices[idx + 23] = sclColor

            vertices[idx + 24] = x4
            vertices[idx + 25] = y4
            vertices[idx + 26] = z
            vertices[idx + 27] = color
            vertices[idx + 28] = u2
            vertices[idx + 29] = v
            vertices[idx + 30] = mixColor
            vertices[idx + 31] = sclColor
        } else {
            val fx2 = x + width
            val fy2 = y + height

            vertices[idx] = x
            vertices[idx + 1] = y
            vertices[idx + 2] = z
            vertices[idx + 3] = color
            vertices[idx + 4] = u
            vertices[idx + 5] = v
            vertices[idx + 6] = mixColor
            vertices[idx + 7] = sclColor

            vertices[idx + 8] = x
            vertices[idx + 9] = fy2
            vertices[idx + 10] = z
            vertices[idx + 11] = color
            vertices[idx + 12] = u
            vertices[idx + 13] = v2
            vertices[idx + 14] = mixColor
            vertices[idx + 15] = sclColor

            vertices[idx + 16] = fx2
            vertices[idx + 17] = fy2
            vertices[idx + 18] = z
            vertices[idx + 19] = color
            vertices[idx + 20] = u2
            vertices[idx + 21] = v2
            vertices[idx + 22] = mixColor
            vertices[idx + 23] = sclColor

            vertices[idx + 24] = fx2
            vertices[idx + 25] = y
            vertices[idx + 26] = z
            vertices[idx + 27] = color
            vertices[idx + 28] = u2
            vertices[idx + 29] = v
            vertices[idx + 30] = mixColor
            vertices[idx + 31] = sclColor
        }
        afterVert?.invoke(this)
    }

    //region request sorting
    private fun sortRequests() {
        if (multithreaded) {
            sortRequestsThreaded()
        } else {
            sortRequestsStandard()
        }
    }

    private fun sortRequestsThreaded() {
        val numRequests = this.numRequests
        val itemZ = requestZ

        var contiguous = this.contiguous
        var ci = 0
        var cl = contiguous.size
        var z = itemZ[0]
        var startI = 0
        // Point3: <z, index, length>
        for (i in 1..<numRequests) {
            if (itemZ[i] != z) { // if contiguous section should end
                contiguous[ci] = z
                contiguous[ci + 1] = startI
                contiguous[ci + 2] = i - startI
                ci += 3
                if (ci + 3 > cl) {
                    contiguous = contiguous.copyOf(1.let { cl = cl shl it; cl })
                }
                z = itemZ[i.also { startI = it }]
            }
        }
        contiguous[ci] = z
        contiguous[ci + 1] = startI
        contiguous[ci + 2] = numRequests - startI
        this.contiguous = contiguous

        val l = (ci / 3) + 1

        if (contiguousCopy.size < contiguous.size) this.contiguousCopy = IntArray(contiguous.size)

        val sorted = CountingSort.countingSortMapMT(contiguous, contiguousCopy, l)


        val locs = contiguous
        locs[0] = 0
        var i = 0
        var ptr = 0
        while (i < l) {
            ptr += sorted[i * 3 + 2]
            locs[i + 1] = ptr
            i++
        }
        if (copy.size < requests.size) copy = arrayOfNulls(requests.size)
        PopulateTask.tasks = sorted
        PopulateTask.src = requests
        PopulateTask.dest = copy
        PopulateTask.locs = locs
        commonPool!!.pool.invoke(PopulateTask(0, l))
    }

    private fun sortRequestsStandard() { // Non-threaded implementation for weak devices
        val numRequests = this.numRequests
        val itemZ = requestZ
        var contiguous = this.contiguous
        var ci = 0
        var cl = contiguous.size
        var z = itemZ[0]
        var startI = 0
        // Point3: <z, index, length>
        for (i in 1..<numRequests) {
            if (itemZ[i] != z) { // if contiguous section should end
                contiguous[ci] = z
                contiguous[ci + 1] = startI
                contiguous[ci + 2] = i - startI
                ci += 3
                if (ci + 3 > cl) {
                    contiguous = contiguous.copyOf(1.let { cl = cl shl it; cl })
                }
                z = itemZ[i.also { startI = it }]
            }
        }
        contiguous[ci] = z
        contiguous[ci + 1] = startI
        contiguous[ci + 2] = numRequests - startI
        this.contiguous = contiguous

        val
                l = (ci / 3) + 1

        if (contiguousCopy.size < contiguous.size) contiguousCopy = IntArray(contiguous.size)

        val sorted = CountingSort.countingSortMap(contiguous, contiguousCopy, l)

        if (copy.size < numRequests) copy = arrayOfNulls(numRequests + (numRequests shr 3))
        var ptr = 0
        val items = requests
        val dest = copy
        var i = 0
        while (i < l * 3) {
            val pos = sorted[i + 1]
            val length = sorted[i + 2]
            if (length < 10) {
                val end = pos + length
                var sj = pos
                var dj = ptr
                while (sj < end) {
                    dest[dj] = items[sj]
                    sj++
                    dj++
                }
            } else System.arraycopy(items, pos, dest, ptr, min(length, dest.size - ptr))
            ptr += length
            i += 3
        }
    }

    internal object CountingSort {
        private val processors = Runtime.getRuntime().availableProcessors() * 8

        var locs: IntArray = IntArray(100)
        val locses: Array<IntArray?> = Array(processors) { IntArray(100) }

        val countses: Array<IntIntMap?> = arrayOfNulls(processors)

        private var entries = arrayOfNulls<Point2>(100)

        private var entries3 = IntArray(300)
        private var entries3a = IntArray(300)
        private var entriesBacking = arrayOfNulls<Int>(100)

        private val tasks: Array<CountingSortTask?> = arrayOfNulls<CountingSortTask>(processors)
        private val task2s = arrayOfNulls<CountingSortTask2>(processors)
        private val futures = arrayOfNulls<Future<*>>(processors)

        init {
            for (i in countses.indices) countses[i] = IntIntMap()
            for (i in entries.indices) entries[i] = Point2()

            for (i in 0..<processors) {
                tasks[i] = CountingSortTask()
                task2s[i] = CountingSortTask2()
            }
        }

        fun countingSortMapMT(arr: IntArray, swap: IntArray, end: Int): IntArray {
            val countses = countses
            val locs = locses
            val threads = min(processors, (end + 4095) / 4096) // 4096 Point3s to process per thread
            val threadSize = end / threads + 1
            val tasks = tasks
            val task2s = task2s
            val futures = futures
            CountingSortTask2.src = arr
            CountingSortTask.arr = CountingSortTask2.src
            CountingSortTask2.dest = swap

            run {
                var s = 0
                var thread = 0
                while (thread < threads) {
                    val task = tasks[thread]!!
                    val stop = min(s + threadSize, end)
                    task.set(s, stop, thread)
                    task2s[thread]!!.set(s, stop, thread)
                    futures[thread] = commonPool!!.pool.submit(task)
                    thread++
                    s += threadSize
                }
            }

            var unique = 0
            for (i in 0..<threads) {
                try {
                    futures[i]!!.get()
                } catch (e: ExecutionException) {
                    commonPool!!.pool.execute(tasks[i]!!)
                } catch (e: InterruptedException) {
                    commonPool!!.pool.execute(tasks[i]!!)
                }
                unique += countses[i]!!.size
            }

            val l = unique
            if (entriesBacking.size < l) {
                entriesBacking = arrayOfNulls(l * 3 / 2)
                entries3 = IntArray(l * 3 * 3 / 2)
                entries3a = IntArray(l * 3 * 3 / 2)
            }
            val entries = entries3
            val entries3a = entries3a
            val entriesBacking = entriesBacking
            var j = 0
            for (i in 0..<threads) {
                if (countses[i]!!.size == 0) continue
                val countEntries = countses[i]!!.entries()
                val entry = countEntries.next()
                entries[j] = entry.key
                entries[j + 1] = entry.value
                entries[j + 2] = i
                j += 3
                while (countEntries.hasNext) {
                    countEntries.next()
                    entries[j] = entry.key
                    entries[j + 1] = entry.value
                    entries[j + 2] = i
                    j += 3
                }
            }

            for (i in 0..<l) {
                entriesBacking[i] = i
            }
            Arrays.sort<Int?>(entriesBacking, 0, l, Structs.comparingInt<Int?>(Intf { i: Int? -> entries[i!! * 3] }))
            for (i in 0..<l) {
                val from = entriesBacking[i]!! * 3
                val to = i * 3
                entries3a[to] = entries[from]
                entries3a[to + 1] = entries[from + 1]
                entries3a[to + 2] = entries[from + 2]
            }

            run {
                var i = 0
                var pos = 0
                while (i < l * 3) {
                    pos =
                        (pos.let { locs[entries3a[i + 2]]!![entries3a[i + 1]] += it; locs[entries3a[i + 2]]!![entries3a[i + 1]] })
                    i += 3
                }
            }

            for (thread in 0..<threads) {
                futures[thread] = commonPool!!.pool.submit(task2s[thread]!!)
            }
            for (i in 0..<threads) {
                try {
                    futures[i]!!.get()
                } catch (e: ExecutionException) {
                    commonPool!!.pool.execute(task2s[i]!!)
                } catch (e: InterruptedException) {
                    commonPool!!.pool.execute(task2s[i]!!)
                }
            }
            return swap
        }

        fun countingSortMap(arr: IntArray, swap: IntArray, end: Int): IntArray {
            var locs = locs
            val counts = countses[0]
            counts!!.clear()

            var unique = 0
            val end3 = end * 3
            run {
                var i = 0
                while (i < end3) {
                    val loc = counts.getOrPut(arr[i], unique)
                    arr[i] = loc
                    if (loc == unique) {
                        if (unique >= locs.size) {
                            locs = locs.copyOf(unique * 3 / 2)
                        }
                        locs[unique++] = 1
                    } else {
                        locs[loc]++
                    }
                    i += 3
                }
            }
            CountingSort.locs = locs

            if (entries.size < unique) {
                val prevLength = entries.size
                entries = entries.copyOf(unique * 3 / 2)
                val entries = entries
                for (i in prevLength..<entries.size) entries[i] = Point2()
            }
            val entries = entries

            val countEntries = counts.entries()
            val entry = countEntries.next()
            entries[0]!!.set(entry.key, entry.value)
            var j = 1
            while (countEntries.hasNext) {
                countEntries.next() // it returns the same entry over and over again.
                entries[j++]!!.set(entry.key, entry.value)
            }
            Arrays.sort<Point2?>(entries, 0, unique, Structs.comparingInt<Point2?>(Intf { p: Point2? -> p!!.x }))

            var prev = entries[0]!!.y
            var next: Int
            for (i in 1..<unique) {
                locs[entries[i]!!.y.also { next = it }] += locs[prev]
                prev = next
            }
            var i = end - 1
            var i3 = i * 3
            while (i >= 0) {
                val destPos = --locs[arr[i3]] * 3
                swap[destPos] = arr[i3]
                swap[destPos + 1] = arr[i3 + 1]
                swap[destPos + 2] = arr[i3 + 2]
                i--
                i3 -= 3
            }
            return swap
        }

        internal class CountingSortTask : Runnable {
            var start: Int = 0
            var end: Int = 0
            var id: Int = 0

            fun set(start: Int, end: Int, id: Int) {
                this.start = start
                this.end = end
                this.id = id
            }

            override fun run() {
                val id = this.id
                val start = this.start
                val end = this.end
                var locs = locses[id]
                val arr = arr
                val counts = countses[id]!!
                counts.clear()
                var unique = 0
                for (i in start..<end) {
                    arr!!
                    locs!!
                    val loc = counts.getOrPut(arr[i * 3], unique)
                    arr[i * 3] = loc
                    if (loc == unique) {
                        if (unique >= locs.size) {
                            locs = locs.copyOf(unique * 3 / 2)
                        }
                        locs[unique++] = 1
                    } else {
                        locs[loc]++
                    }
                }
                locses[id] = locs
            }

            companion object {
                var arr: IntArray? = null
            }
        }

        internal class CountingSortTask2 : Runnable {
            var start: Int = 0
            var end: Int = 0
            var id: Int = 0

            fun set(start: Int, end: Int, id: Int) {
                this.start = start
                this.end = end
                this.id = id
            }

            override fun run() {
                val start = this.start
                val end = this.end
                val locs = locses[id]
                val src = src
                val dest = dest
                var i = end - 1
                var i3 = i * 3
                locs!!
                while (i >= start) {
                    val destPos = --locs[src[i3]] * 3
                    dest[destPos] = src[i3]
                    dest[destPos + 1] = src[i3 + 1]
                    dest[destPos + 2] = src[i3 + 2]
                    i--
                    i3 -= 3
                }
            }

            companion object {
                lateinit var src: IntArray
                lateinit var dest: IntArray
            }
        }
    }

    internal class PopulateTask : RecursiveAction {
        var from: Int = 0
        var to: Int = 0

        //private static final int threshold = 256;
        constructor(from: Int, to: Int) {
            this.from = from
            this.to = to
        }

        constructor()

        override fun compute() {
            val locs = locs
            if (to - from > 1 && locs[to] - locs[from] > 2048) {
                val half = (locs[to] + locs[from]) shr 1
                var mid = Arrays.binarySearch(locs, from, to, half)
                if (mid < 0) mid = -mid - 1
                if (mid != from && mid != to) {
                    ForkJoinTask.invokeAll(PopulateTask(from, mid), PopulateTask(mid, to))
                    return
                }
            }
            val src = src
            val dest = dest
            val tasks = tasks
            for (i in from..<to) {
                val point = i * 3
                val pos = tasks[point + 1]
                val length = tasks[point + 2]
                if (length < 10) {
                    val end = pos + length
                    var sj = pos
                    var dj = locs[i]
                    while (sj < end) {
                        dest[dj] = src[sj]
                        sj++
                        dj++
                    }
                } else {
                    System.arraycopy(src, pos, dest, locs[i], min(length, dest.size - locs[i]))
                }
            }
        }

        companion object {
            lateinit var tasks: IntArray
            lateinit var src: Array<DrawRequest?>
            lateinit var dest: Array<DrawRequest?>
            lateinit var locs: IntArray
        }
    } //endregion

    companion object {
        //xyz + color + uv + mix_color + scl_color
        const val VERTEX_SIZE: Int = 3 + 1 + 2 + 1 + 1
        const val SPRITE_SIZE: Int = 4 * VERTEX_SIZE

        private const val INITIAL_SIZE = 10000
        private val emptyVertices = FloatArray(0)

        var commonPool: ForkJoinHolder? = null

        fun createShader(): Shader {
            return OShader("batch/zbatch", "batch/zbatch").setup()
        }

        fun createDepthShader(): Shader {
            return OShader("batch/zbatchDepth", "batch/zbatchDepth").setup()
        }
    }
}

