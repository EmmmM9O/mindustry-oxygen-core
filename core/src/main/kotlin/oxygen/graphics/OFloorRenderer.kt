package oxygen.graphics

import arc.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.game.EventType.WorldLoadEvent
import mindustry.graphics.*
import mindustry.world.*
import mindustry.world.blocks.environment.*
import oxygen.*
import oxygen.Oxygen.lightCam
import oxygen.Oxygen.lightDir
import oxygen.graphics.g2d.*
import oxygen.math.*
import oxygen.world.*
import kotlin.math.*

/**
 * general implementation:
 *
 * caching:
 * 1. create fixed-size float array for rendering into
 * 2. for each chunk, cache each layer into buffer; record layer boundary indices (alternatively, create mesh per layer for fast recache)
 * 3. create mesh for this chunk based on buffer size, copy buffer into mesh
 *
 * rendering:
 * 1. iterate through visible chunks
 * 2. activate the shader vertex attributes beforehand
 * 3. bind each mesh individually, draw it
 *
 */
class OFloorRenderer : FloorRendererI() {
    val _vertexBuffer: FloatArray = FloatArray(maxSprites * vertexSize * 4)
    private val batch = FloorRenderBatch()
    private val shader: Shader
    private val depthShader: Shader
    private val combinedMat = Mat3D()
    private val tmpMat = Mat3D()
    private var texture: Texture? = null
    private var error: TextureRegion? = null

    val _indexData: IndexData

    private val underwaterDraw = Seq<Runnable>(Runnable::class.java)

    //alpha value of pixels cannot exceed the alpha of the surface they're being drawn on
    private val underwaterBlend = Blending(
        Gl.srcAlpha, Gl.oneMinusSrcAlpha,
        Gl.dstAlpha, Gl.oneMinusSrcAlpha
    )

    var drawDepth = false

    override fun getIndexData(): IndexData = _indexData

    override fun getVertexBuffer(): FloatArray = _vertexBuffer

    init {
        var j: Short = 0
        val indices = ShortArray(maxSprites * 6)
        var i = 0
        while (i < indices.size) {
            indices[i] = j
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j
            i += 6
            j = (j + 4).toShort()
        }

        _indexData = object : IndexBufferObject(true, indices.size) {
            override fun dispose() {
                //there is never a need to dispose this index buffer
            }
        }
        _indexData.set(indices, 0, indices.size)

        shader = OShader("batch/ofloorBatch", "batch/ofloorBatch").setup()

        depthShader = OShader("batch/ofloorDepth", "3d/depth").setup()

        Events.on(WorldLoadEvent::class.java) { reloadTexture() }
    }

    public fun getShader() = if (drawDepth) depthShader else shader

    fun getData(tiles: Tiles): TilesRenderData = Oxygen.renderer.getData(tiles)

    /** Queues up a cache change for a tile. Only runs in render loop.  */
    public override fun recacheTile(tile: Tile) {
        recacheTile(tile.tiles, tile.x.toInt(), tile.y.toInt())
    }

    public override fun recacheTile(x: Int, y: Int) {
        recacheTile(Vars.world.tiles, x, y)
    }

    fun recacheTile(tiles: Tiles, x: Int, y: Int) {
        getData(tiles).recacheSet.add(Point2.pack(x / chunksize, y / chunksize))
    }

    fun recacheTile(data: TilesRenderData, x: Int, y: Int) {
        data.recacheSet.add(Point2.pack(x / chunksize, y / chunksize))
    }

    public override fun drawFloor() {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            drawFloor(getData(tiles))
        }
    }

    fun drawFloor(data: TilesRenderData) {
        val tiles = data.tiles
        val cache = data.cache
        if (cache == null) return

        val camera = Core.camera

        val pad = Vars.tilesize / 2f

        val bounds = camera.bounds(Tmp.r3)
        TilesHandler.rect(bounds, tiles.craft.inv())

        val minx = max(((bounds.x - pad) / chunkunits).toInt(), 0)
        val miny = max(((bounds.y - pad) / chunkunits).toInt(), 0)
        val maxx = min(Mathf.ceil((bounds.x + bounds.width + pad) / chunkunits), cache!!.size)
        val maxy = min(Mathf.ceil((bounds.y + bounds.height + pad) / chunkunits), cache!![0]!!.size)

        val layers = CacheLayer.all.size

        data.drawnLayers.clear()
        data.drawnLayerSet.clear()


        //preliminary layer check
        for (x in minx..maxx) {
            for (y in miny..maxy) {
                if (!Structs.inBounds<Array<ChunkMesh?>?>(x, y, cache)) continue

                if (cache!![x]!![y]!!.isEmpty()) {
                    cacheChunk(data, x, y, false)
                }

                val chunk: Array<ChunkMesh?> = cache!![x]!![y]!!

                //loop through all layers, and add layer index if it exists 
                for (i in 0..<layers) {
                    if (chunk[i] != null && i != CacheLayer.walls.id && chunk[i]!!.bounds.overlaps(bounds)) {
                        data.drawnLayerSet.add(i)
                    }
                }
            }
        }

        val it = data.drawnLayerSet.iterator()
        while (it.hasNext) {
            data.drawnLayers.add(it.next())
        }

        data.drawnLayers.sort()

        beginDraw(data)

        for (i in 0..<data.drawnLayers.size) {
            drawLayer(data, CacheLayer.all[data.drawnLayers.get(i)])
        }

        //underwaterDraw.clear()
    }

    public override fun checkChanges() {
        checkChanges(false)
    }

    public override fun checkChanges(ignoreWalls: Boolean) {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            checkChanges(tiles, ignoreWalls)
        }
    }

    fun checkChanges(tiles: Tiles, ignoreWalls: Boolean) {
        getData(tiles).apply {
            if (recacheSet.size > 0) {
                //recache one chunk at a time
                val iterator = recacheSet.iterator()
                while (iterator.hasNext) {
                    val chunk = iterator.next()
                    cacheChunk(this, Point2.x(chunk).toInt(), Point2.y(chunk).toInt(), ignoreWalls)
                }

                recacheSet.clear()
            }
        }
    }

    public override fun drawUnderwater(run: Runnable?) {
        underwaterDraw.add(run)
    }

    var realZ = 0f
    fun beginDraw(data: TilesRenderData) {
        val renderer = Oxygen.renderer

        Draw.flush()

        val sh = getShader()
        sh.bind()
        //coordinates of geometry are normalized to [0, 1] based on map size (normWidth/normHeight), so the matrix needs to be updated accordingly
        combinedMat.set(Oxygen.trans3D).mul(data.tiles.craft.trans().to3D(tmpMat))
            .translate(0f, 0f, data.tiles.craft.height() - 1f).translate(-packPad, -packPad, realZ)
            .scale(data.packWidth, data.packHeight, 1f)
        sh.setUniformMatrix4("u_trans", combinedMat.`val`)
        sh.setUniformMatrix4("u_proj", OGraphics.proj3D().`val`)

        if (!drawDepth) {
            sh.setUniformMatrix4("u_lightProj", lightCam.combined.`val`)
            sh.setUniformf("u_lightDir", lightDir)
            renderer.shadowBuffer.texture.bind(1)
            sh.setUniformi("u_shadowMap", 1)
        }

        //only ever use the base environment texture
        texture!!.bind(0)

        Gl.enable(Gl.blend)
    }

    public override fun beginDraw() {
        if (Vars.world.tiles.craft == null) return
        beginDraw(getData(Vars.world.tiles))
    }

    fun drawLayer(data: TilesRenderData, layer: CacheLayer) {
        val camera = Core.camera
        val cache = data.cache

        val bounds = camera.bounds(Tmp.r3)
        TilesHandler.rect(bounds, data.tiles.craft.inv())
        val minx = max(((bounds.x - pad) / chunkunits).toInt(), 0)
        val miny = max(((bounds.y - pad) / chunkunits).toInt(), 0)
        val maxx = min(Mathf.ceil((bounds.x + bounds.width + pad) / chunkunits), cache!!.size)
        val maxy = min(Mathf.ceil((bounds.y + bounds.height + pad) / chunkunits), cache!![0]!!.size)

        layer.begin()

        for (x in minx..maxx) {
            for (y in miny..maxy) {
                if (!Structs.inBounds<Array<ChunkMesh?>?>(x, y, cache) || cache!![x]!![y]!!.isEmpty()) {
                    continue
                }

                val mesh = cache!![x]!![y]!![layer.id]

                if (mesh != null && mesh.bounds.overlaps(bounds)) {
                    mesh.render(getShader(), Gl.triangles, 0, mesh.maxVertices * 6 / 4)
                }
            }
        }

        //every underwater object needs to be drawn once per cache layer, which sucks.
        if (layer.liquid && underwaterDraw.size > 0) {
            Draw.blend(underwaterBlend)

            val items = underwaterDraw.items
            val len = underwaterDraw.size
            for (i in 0..<len) {
                items[i].run()
            }

            Draw.flush()
            Draw.blend(Blending.normal)
            Blending.normal.apply()
            beginDraw(data)
        }

        layer.end()

    }

    public override fun drawLayer(layer: CacheLayer) {
        if (Vars.world.tiles.craft == null) return
        drawLayer(getData(Vars.world.tiles), layer)
    }

    private fun cacheChunk(data: TilesRenderData, cx: Int, cy: Int, ignoreWalls: Boolean) {
        data.apply {
            used.clear()

            var tilex = max(cx * chunksize - 1, 0)
            while (tilex < (cx + 1) * chunksize + 1 && tilex < tiles.width) {
                var tiley = max(cy * chunksize - 1, 0)
                while (tiley < (cy + 1) * chunksize + 1 && tiley < tiles.height) {
                    val tile = tiles.get(tilex, tiley)
                    if (tile == null) continue
                    val wall = !ignoreWalls && tile.block().cacheLayer !== CacheLayer.normal

                    if (wall) {
                        used.add(tile.block().cacheLayer)
                    }

                    if (!wall || tiles.isAccessible(tilex, tiley)) {
                        used.add(tile.floor().cacheLayer)
                    }
                    tiley++
                }
                tilex++
            }

            if (cache!![cx]!![cy]!!.isEmpty()) {
                cache!![cx]!![cy] = arrayOfNulls(CacheLayer.all.size)
            }

            val meshes: Array<ChunkMesh?> = cache!![cx]!![cy]!!

            for (layer in CacheLayer.all) {
                if (meshes[layer.id] != null) {
                    meshes[layer.id]!!.dispose()
                }
                meshes[layer.id] = null
            }

            for (layer in used) {
                meshes[layer.id] = cacheChunkLayer(this, cx, cy, layer, ignoreWalls)
            }
        }
    }

    private fun cacheChunkLayer(
        data: TilesRenderData,
        cx: Int,
        cy: Int,
        layer: CacheLayer?,
        ignoreWalls: Boolean
    ): ChunkMesh {
        batch.vidx = 0
        batch.packWidth = data.packWidth
        batch.packHeight = data.packHeight
        var flag = false
        val current = Core.batch
        if (current == OGraphics.zbatch) {
            OGraphics.zbatch = batch
            flag = true
        }
        Core.batch = batch

        val tiles = data.tiles
        val craft = tiles.craft

        for (tilex in cx * chunksize..<(cx + 1) * chunksize) {
            for (tiley in cy * chunksize..<(cy + 1) * chunksize) {
                val tile = tiles.get(tilex, tiley)
                val floor: Floor

                if (tile == null) {
                    continue
                }
                floor = tile.floor()

                if (tile.block().cacheLayer === layer && layer === CacheLayer.walls && !(tile.isDarkened && tile.data >= 5)) {
                    OGraphics.realZ(4f)
                    tile.block().drawBase(tile)
                    OGraphics.realZ(0f)
                } else if (floor.cacheLayer === layer && (ignoreWalls || tiles.isAccessible(
                        tile.x.toInt(),
                        tile.y.toInt()
                    ) || tile.block().cacheLayer !== CacheLayer.walls || !tile.block().fillsTile)
                ) {
                    floor.drawBase(tile)
                } else if (floor.cacheLayer !== layer && layer !== CacheLayer.walls) {
                    floor.drawNonLayer(tile, layer)
                }
            }
        }

        Core.batch = current
        if (flag) OGraphics.zbatch = current as ZBatch

        val floats = batch.vidx
        val mesh = ChunkMesh(
            true,
            floats / vertexSize,
            0,
            attributes,
            cx * Vars.tilesize * chunksize - Vars.tilesize / 2f,
            cy * Vars.tilesize * chunksize - Vars.tilesize / 2f,
            (cx + 1) * Vars.tilesize * chunksize + Vars.tilesize / 2f,
            (cy + 1) * Vars.tilesize * chunksize + Vars.tilesize / 2f
        )

        mesh.setVertices(_vertexBuffer, 0, floats)
        //all indices are shared and identical
        mesh.indices = _indexData

        return mesh
    }

    fun reload(data: TilesRenderData, ignoreWalls: Boolean) {
        //dispose all old meshes
        data.apply {
            if (cache != null) {
                for (x in cache) {
                    for (y in x!!) {
                        for (mesh in y!!) {
                            mesh?.dispose()
                        }
                    }
                }
            }

            recacheSet.clear()
            val chunksx = Mathf.ceil(tiles.width.toFloat() / chunksize)
            val chunksy = Mathf.ceil(tiles.height.toFloat() / chunksize)
            cache =
                Array<Array<Array<ChunkMesh?>?>?>(chunksx) {
                    Array<Array<ChunkMesh?>?>(chunksy) {
                        arrayOfNulls<ChunkMesh>(
                            if (dynamic) 0 else CacheLayer.all.size
                        )
                    }
                }

            packWidth = tiles.unitWidth().toFloat() + packPad * 2f
            packHeight = tiles.unitHeight().toFloat() + packPad * 2f

            //pre-cache chunks
            if (!dynamic) {
                Time.mark();

                for (x in 0..<chunksx) {
                    for (y in 0..<chunksy) {
                        cacheChunk(this, x, y, ignoreWalls)
                    }
                }

                Log.debug("Load @ mesh: @ms", data.tiles, Time.elapsed())
            }
        }
    }

    fun reloadTexture() {
        texture = Core.atlas.find("grass1").texture
        error = Core.atlas.find("env-error")
    }

    public override fun reload(ignoreWalls: Boolean) {
    }


    internal inner class FloorRenderBatch : ZBatch() {
        var vidx = 0
        var packWidth = 0f
        var packHeight = 0f
        //TODO: alternate clipping approach, can be more accurate
        /*
        float minX, minY, maxX, maxY;

        void reset(){
            minX = Float.POSITIVE_INFINITY;
            minY = Float.POSITIVE_INFINITY;
            maxX = 0f;
            maxY = 0f;
        }
        */
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
            //substitute invalid regions with error
            val z = getDrawZ()
            if (region.texture !== texture && region !== error) {
                draw(error!!, x, y, originX, originY, width, height, rotation)
                return
            }

            val verts: FloatArray = _vertexBuffer
            val idx = vidx
            vidx += spriteSize

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

                val u = region.u
                val v = region.v2
                val u2 = region.u2
                val v2 = region.v

                val color = this.colorPacked

                verts[idx] = pack(x1, y1)
                verts[idx + 1] = z
                verts[idx + 2] = color
                verts[idx + 3] = Pack.packUv(u, v)

                verts[idx + 4] = pack(x2, y2)
                verts[idx + 5] = z
                verts[idx + 6] = color
                verts[idx + 7] = Pack.packUv(u, v2)

                verts[idx + 8] = pack(x3, y3)
                verts[idx + 9] = z
                verts[idx + 10] = color
                verts[idx + 11] = Pack.packUv(u2, v2)

                verts[idx + 12] = pack(x4, y4)
                verts[idx + 13] = z
                verts[idx + 14] = color
                verts[idx + 15] = Pack.packUv(u2, v)
            } else {
                val fx2 = x + width
                val fy2 = y + height
                val u = region.u
                val v = region.v2
                val u2 = region.u2
                val v2 = region.v

                val color = this.colorPacked

                verts[idx] = pack(x, y)
                verts[idx + 1] = z
                verts[idx + 2] = color
                verts[idx + 3] = Pack.packUv(u, v)

                verts[idx + 4] = pack(x, fy2)
                verts[idx + 5] = z
                verts[idx + 6] = color
                verts[idx + 7] = Pack.packUv(u, v2)

                verts[idx + 8] = pack(fx2, fy2)
                verts[idx + 9] = z
                verts[idx + 10] = color
                verts[idx + 11] = Pack.packUv(u2, v2)

                verts[idx + 12] = pack(fx2, y)
                verts[idx + 13] = z
                verts[idx + 14] = color
                verts[idx + 15] = Pack.packUv(u2, v)
            }
        }

        fun pack(x: Float, y: Float): Float {
            return Pack.packUv((x + packPad) / packWidth, (y + packPad) / packHeight)
        }

        override fun flush() {
        }

        override fun setShader(shader: Shader?, apply: Boolean) {
            throw IllegalArgumentException("cache shader unsupported")
        }

        override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
            require(spriteVertices.size == 20) { "cached vertices must be in non-mixcolor format (20 per sprite, 5 per vertex)" }

            val verts: FloatArray = _vertexBuffer
            var idx = vidx
            var sidx = offset
            val z = getDrawZ()

            //convert 5-float format to internal packed 3-float format
            for (i in 0..3) {
                verts[idx++] = pack(spriteVertices[sidx++], spriteVertices[sidx++])
                verts[idx++] = z
                verts[idx++] = spriteVertices[sidx++]
                verts[idx++] = Pack.packUv(spriteVertices[sidx++], spriteVertices[sidx++])
            }

            vidx += spriteSize
        }

        override fun drawImpl(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
            require(spriteVertices.size == 24) { "cached vertices must be in non-mixcolor format (24 per sprite, 6 per vertex)" }

            val verts: FloatArray = _vertexBuffer
            var idx = vidx
            var sidx = offset

            //convert 5-float format to internal packed 3-float format
            for (i in 0..3) {
                verts[idx++] = pack(spriteVertices[sidx++], spriteVertices[sidx++])
                verts[idx++] = spriteVertices[sidx++]
                verts[idx++] = spriteVertices[sidx++]
                verts[idx++] = Pack.packUv(spriteVertices[sidx++], spriteVertices[sidx++])
            }

            vidx += spriteSize
        }
    }

    companion object {
        private val attributes = arrayOf<VertexAttribute>(
            VertexAttribute.packedPosition,
            VertexAttribute(1, "a_z"),
            VertexAttribute.color,
            VertexAttribute.packedTexCoords
        )
        private const val chunksize = 30 //todo 32?
        private const val chunkunits = chunksize * Vars.tilesize
        private const val vertexSize = 1 + 1 + 1 + 1
        private const val spriteSize = vertexSize * 4
        private const val maxSprites = chunksize * chunksize * 9
        private const val packPad = Vars.tilesize * 8f
        private const val pad = Vars.tilesize / 2f

        //if true, chunks are rendered on-demand; this causes small lag spikes and is generally not needed for most maps
        private const val dynamic = false
    }
}

class ChunkMesh(
    isStatic: Boolean,
    maxVertices: Int,
    maxIndices: Int,
    attributes: Array<VertexAttribute>,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float
) : Mesh(isStatic, maxVertices, maxIndices, *attributes) {
    var bounds: Rect = Rect()

    init {
        bounds.set(minX, minY, maxX - minX, maxY - minY)
    }
}
