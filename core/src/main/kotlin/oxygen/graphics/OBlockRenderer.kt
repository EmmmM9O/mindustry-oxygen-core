package oxygen.graphics

import arc.*
import arc.graphics.*
import arc.graphics.Texture.TextureFilter
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.math.*
import arc.math.geom.*
import arc.util.*
import mindustry.*
import mindustry.content.*
import mindustry.game.EventType.*
import mindustry.gen.*
import mindustry.graphics.*
import mindustry.world.*
import mindustry.world.blocks.environment.Floor.UpdateRenderState
import mindustry.world.blocks.power.*
import oxygen.*
import oxygen.math.*
import oxygen.world.*
import oxygen.world.blocks.*
import kotlin.math.*

class OBlockRenderer : BlockRendererI() {
    //TODO cracks take up far to much space, so I had to limit it to 7. this means larger blocks won't have cracks - draw tiling mirrored stuff instead?
    val crackRegions: Int =
        8  //TODO cracks take up far to much space, so I had to limit it to 7. this means larger blocks won't have cracks - draw tiling mirrored stuff instead?
    val maxCrackSize: Int = 7
    var drawQuadtreeDebug: Boolean = false
    val shadowColor: Color = Color(0f, 0f, 0f, 0.71f)
    val blendShadowColor: Color = Color.white.cpy().lerp(Color.black, shadowColor.a)

    private val initialRequests: Int = ORenderer.initialRequests

    lateinit var cracks: Array<Array<TextureRegion>>

    //TODO I don't like this system

    var hadMapLimit = false
    var brokenFade = 0f

    val tmpMat1 = Mat3D()
    val tmpMat2 = Mat3D()

    val tmpM = Mat()
    val tmpV = Vec2()

    val floorL = OFloorRenderer()

    fun getData(tiles: Tiles): TilesRenderData = Oxygen.renderer.getData(tiles)

    init {
        floor = floorL
        Events.on(ClientLoadEvent::class.java) { _: ClientLoadEvent ->
            cracks = Array(maxCrackSize) { size ->
                Array(crackRegions) { region ->
                    Core.atlas.find("cracks-${size + 1}-$region")!!
                }
            }
        }

        Events.on(WorldLoadEvent::class.java) { event: WorldLoadEvent ->
            reload()
        }

        Events.on(TilePreChangeEvent::class.java) { event: TilePreChangeEvent ->
            //if (blockTree == null || floorTree == null) return@Cons
            val tile = event.tile
            if (tile.tiles.craft == null) return@on
            val data = getData(tile.tiles)
            if (indexBlock(tile)) {
                data.blockTree.remove(tile)
                data.blockLightTree.remove(tile)
                if (tile.build != null && tile.build is G3DrawBuilding) data.block3DTree.remove(tile)
            }
            if (indexFloor(tile)) data.floorTree.remove(tile)
        }

        Events.on(TileChangeEvent::class.java) { event: TileChangeEvent ->
            val tile = event.tile
            val visible = tile.build == null || !tile.build.inFogTo(Vars.player.team())
            if (tile.build != null) {
                tile.build.wasVisible = visible
            }
            if (tile.tiles == null) return@on
            if (tile.tiles.craft == null) return@on
            val data = getData(tile.tiles)

            if (visible) {
                data.shadowEvents.add(event.tile)
            }

            invalidateTile(tile)
            recordIndex(tile)
        }
    }

    fun reload(data: TilesRenderData) {
        data.apply {
            blockTree = BlockQuadtree(Rect(0f, 0f, tiles.unitWidth().toFloat(), tiles.unitHeight().toFloat()))
            blockLightTree =
                BlockLightQuadtree(Rect(0f, 0f, tiles.unitWidth().toFloat(), tiles.unitHeight().toFloat()))
            block3DTree =
                Block3DQuadtree(Rect(0f, 0f, tiles.unitWidth().toFloat(), tiles.unitHeight().toFloat()))

            floorTree = FloorQuadtree(Rect(0f, 0f, tiles.unitWidth().toFloat(), tiles.unitHeight().toFloat()))
            shadowEvents.clear()
            updateFloors.clear()

            shadows.texture.setFilter(TextureFilter.linear, TextureFilter.linear)
            shadows.resize(tiles.width + 2, tiles.height + 2)
            shadows.begin()
            Core.graphics.clear(Color.white)
            Draw.proj().setOrtho(0f, 0f, tiles.width.toFloat() + 2f, tiles.height.toFloat() + 2f)

            Draw.color(blendShadowColor)

            for (tile in tiles) {
                recordIndex(tile)

                if (tile.floor().updateRender(tile)) {
                    updateFloors.add(UpdateRenderState(tile, tile.floor()))
                }

                if (tile.overlay().updateRender(tile)) {
                    updateFloors.add(UpdateRenderState(tile, tile.overlay()))
                }

                if (tile.build != null && (tile.team() === Vars.player.team() || !Vars.state.rules.fog || (tile.build.visibleFlags and (1L shl Vars.player.team().id)) != 0L)) {
                    tile.build.wasVisible = true
                }

                if (tile.block().displayShadow(tile) && (tile.build == null || tile.build.wasVisible)) {
                    Fill.rect(tile.x + 1.5f, tile.y + 1.5f, 1f, 1f)
                }
            }

            Draw.flush()
            Draw.color()
            shadows.end()

            //TODO
            if (tiles == Vars.world.tiles) updateDarkness(this)
        }
    }

    override fun reload() {
        hadMapLimit = Vars.state.rules.limitMapArea
    }

    fun updateShadows(data: TilesRenderData, ignoreBuildings: Boolean, ignoreTerrain: Boolean) {
        data.apply {
            shadows.texture.setFilter(TextureFilter.linear, TextureFilter.linear)
            shadows.resize(tiles.width, tiles.height)
            shadows.begin()
            Core.graphics.clear(Color.white)
            Draw.proj().setOrtho(0f, 0f, tiles.width.toFloat() + 2f, tiles.height.toFloat() + 2f)

            Draw.color(blendShadowColor)

            for (tile in tiles) {
                if (tile.block()
                        .displayShadow(tile) && (tile.build == null || tile.build.wasVisible) && !(ignoreBuildings && !tile.block().isStatic) && !(ignoreTerrain && tile.block().isStatic)
                ) {
                    Fill.rect(tile.x + 1.5f, tile.y + 1.5f, 1f, 1f)
                }
            }

            Draw.flush()
            Draw.color()
            shadows.end()
        }
    }

    override fun updateShadows(ignoreBuildings: Boolean, ignoreTerrain: Boolean) {
        updateShadows(getData(Vars.world.tiles), ignoreBuildings, ignoreTerrain)
    }

    fun updateDarkness(data: TilesRenderData) {
        data.apply {
            val darkFbo = dark
            if (darkFbo == null) return@apply
            darkEvents.clear()
            darkFbo.texture.setFilter(TextureFilter.linear)
            darkFbo.resize(Vars.world.width(), Vars.world.height())
            darkFbo.begin()

            //fill darkness with black when map area is limited
            Core.graphics.clear(if (Vars.state.rules.limitMapArea && tiles == Vars.world.tiles) Color.black else Color.white)
            Draw.proj().setOrtho(0f, 0f, darkFbo.width.toFloat(), darkFbo.height.toFloat())

            //clear out initial starting area
            if (Vars.state.rules.limitMapArea && tiles == Vars.world.tiles) {
                Draw.color(Color.white)
                Fill.crect(
                    Vars.state.rules.limitX.toFloat(),
                    Vars.state.rules.limitY.toFloat(),
                    Vars.state.rules.limitWidth.toFloat(),
                    Vars.state.rules.limitHeight.toFloat()
                )
            }

            for (tile in tiles) {
                //skip lighting outside rect
                if (Vars.state.rules.limitMapArea && tiles == Vars.world.tiles && !Rect.contains(
                        Vars.state.rules.limitX.toFloat(),
                        Vars.state.rules.limitY.toFloat(),
                        (Vars.state.rules.limitWidth - 1).toFloat(),
                        (Vars.state.rules.limitHeight - 1).toFloat(),
                        tile.x.toFloat(),
                        tile.y.toFloat()
                    )
                ) {
                    continue
                }

                val darkness = tiles.getDarkness(tile.x.toInt(), tile.y.toInt())

                if (darkness > 0) {
                    val dark = 1f - min((darkness + 0.5f) / 4f, 1f)
                    Draw.colorl(dark)
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
                }
            }

            Draw.flush()
            Draw.color()
            darkFbo.end()
        }
    }

    override fun updateDarkness() {
        updateDarkness(getData(Vars.world.tiles))
    }

    override fun invalidateTile(tile: Tile) {
    }

    override fun getShadowBuffer(): FrameBuffer {
        return getData(Vars.world.tiles).shadows
    }

    override fun removeFloorIndex(tile: Tile) {
        if (indexFloor(tile)) getData(tile.tiles).floorTree.remove(tile)
    }

    override fun addFloorIndex(tile: Tile) {
        if (indexFloor(tile)) getData(tile.tiles).floorTree.insert(tile)
    }

    fun indexBlock(tile: Tile): Boolean {
        val block = tile.block()
        return tile.isCenter && block !== Blocks.air && block.cacheLayer === CacheLayer.normal
    }

    fun indexFloor(tile: Tile): Boolean {
        return tile.block() === Blocks.air && tile.floor().emitLight && tile.tiles.getDarkness(
            tile.x.toInt(),
            tile.y.toInt()
        ) < 3
    }

    fun recordIndex(tile: Tile) {
        val data = getData(tile.tiles)
        if (indexBlock(tile)) {
            data.blockTree.insert(tile)
            data.blockLightTree.insert(tile)
            if (tile.build is G3DrawBuilding) data.block3DTree.insert(tile)
        }
        if (indexFloor(tile)) data.floorTree.insert(tile)
    }

    override fun recacheWall(tile: Tile) {
        val data = getData(tile.tiles)
        for (cx in tile.x - Vars.darkRadius..tile.x + Vars.darkRadius) {
            for (cy in tile.y - Vars.darkRadius..tile.y + Vars.darkRadius) {
                val other = tile.tiles.get(cx, cy)
                if (other != null) {
                    data.darkEvents.add(other.pos())
                    floorL.recacheTile(data, other.x.toInt(), other.y.toInt())
                }
            }
        }
    }

    fun checkChanges(data: TilesRenderData) {
        data.apply {
            darkEvents.each { pos: Int ->
                val tile = tiles.getp(pos)
                if (tile != null && tile.block().fillsTile) {
                    tile.data = tiles.getWallDarkness(tile)
                }
            }
        }
    }

    fun checkChanges() {
        //TODO
        checkChanges(getData(Vars.world.tiles))
    }

    fun processDarkness(data: TilesRenderData) {
        data.apply {
            if (!darkEvents.isEmpty) {
                val darkFbo = dark
                if (darkFbo == null) return@apply
                Draw.flush()

                darkFbo.begin()
                Draw.proj().setOrtho(0f, 0f, darkFbo.width.toFloat(), darkFbo.height.toFloat())

                darkEvents.each({ pos: Int ->
                    val tile = tiles.getp(pos) ?: return@each
                    val darkness = tiles.getDarkness(tile.x.toInt(), tile.y.toInt())
                    //then draw the shadow
                    Draw.colorl(if (darkness <= 0f) 1f else 1f - min((darkness + 0.5f) / 4f, 1f))
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
                })

                Draw.flush()
                Draw.color()
                darkFbo.end()
                darkEvents.clear()

                Draw.proj(Core.camera)
            }
        }
    }

    fun processDarkness() {
        processDarkness(getData(Vars.world.tiles))
    }

    fun drawDarkness(data: TilesRenderData) {
        //TODO
        data.apply {
            val darkFbo = dark
            if (darkFbo == null) return@apply
            Draw.shader(OCShaders.darkness)
            Draw.fbo(darkFbo.texture, Vars.world.width(), Vars.world.height(), Vars.tilesize, Vars.tilesize / 2f)
            Draw.shader()
        }
    }

    fun drawDarkness() {
        drawDarkness(getData(Vars.world.tiles))
    }

    /*
    fun drawDestroyed() {
        if (!Core.settings.getBool("destroyedblocks")) return

        brokenFade =
            if (Vars.control.input.isPlacing || Vars.control.input.isBreaking || (Vars.control.input.isRebuildSelecting && !Core.scene.hasKeyboard())) {
                Mathf.lerpDelta(brokenFade, 1f, 0.1f)
            } else {
                Mathf.lerpDelta(brokenFade, 0f, 0.1f)
            }

        if (brokenFade > 0.001f) {
            for (block in Vars.player.team().data().plans) {
                val b = block.block
                if (!Core.camera.bounds(Tmp.r1).grow(Vars.tilesize * 2f).overlaps(
                        Tmp.r2.setSize((b.size * Vars.tilesize).toFloat())
                            .setCenter(block.x * Vars.tilesize + b.offset, block.y * Vars.tilesize + b.offset)
                    )
                ) continue

                Draw.alpha(0.33f * brokenFade)
                Draw.mixcol(Color.white, 0.2f + Mathf.absin(Time.globalTime, 6f, 0.2f))
                Draw.rect(
                    b.fullIcon,
                    block.x * Vars.tilesize + b.offset,
                    block.y * Vars.tilesize + b.offset,
                    if (b.rotate) (block.rotation * 90).toFloat() else 0f
                )
            }
            Draw.reset()
        }
    }*/

    fun processShadows(data: TilesRenderData) {
        data.apply {
            if (!shadowEvents.isEmpty) {
                Draw.flush()

                shadows.begin()
                Draw.proj().setOrtho(0f, 0f, tiles.width.toFloat() + 2f, tiles.height.toFloat() + 2f)

                for (tile in shadowEvents) {
                    if (tile == null) continue
                    //draw white/shadow color depending on blend
                    Draw.color(
                        if (!tile.block()
                                .displayShadow(tile) || (Vars.state.rules.fog && tile.build != null && !tile.build.wasVisible)
                        ) Color.white else blendShadowColor
                    )
                    Fill.rect(tile.x + 1.5f, tile.y + 1.5f, 1f, 1f)
                }

                Draw.flush()
                Draw.color()
                shadows.end()
                shadowEvents.clear()

                Draw.proj(Core.camera)
            }
        }
    }

    override fun processShadows() {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            processShadows(getData(tiles))
        }
    }

    fun drawShadows(data: TilesRenderData) {
        data.apply {
            val MAX = 25
            val craft = tiles.craft
            if (tiles.width <= MAX && tiles.height <= MAX) {
                tmpMat1.set(OGraphics.trans3D())
                tmpM.set(Draw.trans())
                OGraphics.trans3D().translate(0f, 0f, data.tiles.craft.height())
                Draw.trans(craft.trans())
                Tmp.tr1.set(shadows.texture)
                Tmp.tr1.set(0f, 1f, 1f, 0f)

                Draw.shader(OCShaders.darkness)
                Draw.rect(
                    Tmp.tr1,
                    tiles.unitWidth().toFloat() / 2f - Vars.tilesize / 2f,
                    tiles.unitHeight().toFloat() / 2f - Vars.tilesize / 2f,
                    tiles.unitWidth().toFloat() + 2f * Vars.tilesize,
                    tiles.unitHeight().toFloat() + 2f * Vars.tilesize
                )
                Draw.shader()

                OGraphics.trans3D(tmpMat1)
                Draw.trans(tmpM)
            } else {
                //TODO
                val ww = (tiles.width * Vars.tilesize + Vars.tilesize * 2).toFloat()
                val wh = (tiles.height * Vars.tilesize + Vars.tilesize * 2).toFloat()
                val bounds = Core.camera.bounds(Tmp.r3).grow(Vars.tilesize * 2f)
                TilesHandler.rect(bounds, craft.inv())
                val x = bounds.x + Vars.tilesize * 1.5f
                val y = bounds.y + Vars.tilesize * 1.5f
                val u = x / ww
                val v = y / wh
                val u2 = (x + bounds.width) / ww
                val v2 = (y + bounds.height) / wh

                Tmp.tr1.set(shadows.texture)
                Tmp.tr1.set(u, v2, u2, v)
                tmpMat1.set(OGraphics.trans3D())
                tmpM.set(Draw.trans())
                OGraphics.trans3D().translate(0f, 0f, data.tiles.craft.height())
                Draw.trans(craft.trans())

                Draw.shader(OCShaders.darkness)
                Draw.rect(
                    Tmp.tr1,
                    bounds.x + bounds.width / 2,
                    bounds.y + bounds.height / 2,
                    bounds.width,
                    bounds.height
                )
                Draw.shader()

                OGraphics.trans3D(tmpMat1)
                Draw.trans(tmpM)
            }
        }
    }

    fun drawShadows() {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            drawShadows(getData(tiles))
        }
    }

    fun processBlocks() {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            processBlocks(getData(tiles))
        }
    }

    /** Process all blocks to draw.  */
    fun processBlocks(data: TilesRenderData) {
        data.apply {
            if (!Vars.state.isPaused) {
                val updates = updateFloors.size
                val uitems = updateFloors.items
                for (i in 0..<updates) {
                    val tile: UpdateRenderState = uitems[i]!!
                    tile.floor.renderUpdate(tile)
                }
            }

            val craft = tiles.craft

            tileview.clear()
            lightview.clear()
            g3dview.clear()
            procLinks.clear()
            procLights.clear()
            proc3D.clear()

            val bounds = Core.camera.bounds(Tmp.r3).grow(Vars.tilesize * 2f)
            TilesHandler.rect(bounds, craft.inv())

            //draw floor lights
            floorTree.intersect(bounds) { value: Tile -> lightview.add(value) }

            blockLightTree.intersect(bounds) { tile: Tile ->
                if (tile.block().emitLight && (tile.build == null || procLights.add(tile.build.pos()))) {
                    lightview.add(tile)
                }
            }

            block3DTree.intersect(bounds) { tile: Tile ->
                if (tile.build != null && tile.build is G3DrawBuilding && proc3D.add(tile.build.id)) {
                    g3dview.add(tile)
                }
            }

            blockTree.intersect(bounds) { tile: Tile ->
                if (tile.build == null || procLinks.add(tile.build.id)) {
                    tileview.add(tile)
                }
                if (tile.build != null && tile.build.power != null && tile.build.power.links.size > 0) {
                    for (other in tile.build.getPowerConnections(outArray2)) {
                        if (other.block is PowerNode && procLinks.add(other.id)) { //TODO need a generic way to render connections!
                            tileview.add(other.tile)
                        }
                    }
                }
            }
        }
    }

    //debug method for drawing block bounds
    /*
    fun drawTree(tree: QuadTree<Tile>) {
        Draw.color(Color.blue)
        Lines.rect(tree.bounds)

        Draw.color(Color.green)
        for (tile in tree.objects) {
            val block = tile.block()
            Tmp.r1.setCentered(
                tile.worldx() + block.offset,
                tile.worldy() + block.offset,
 block.clipSize,
                block.clipSize
            )
            Lines.rect(Tmp.r1)
        }

        if (!tree.leaf) {
            drawTree(tree.botLeft)
            drawTree(tree.botRight)
            drawTree(tree.topLeft)
            drawTree(tree.topRight)
        }
        Draw.reset()
    }*/

    fun g3dEach(data: TilesRenderData, cons: (G3DrawBuilding) -> Unit) {
        tmpMat1.set(Oxygen.trans3D)
        Oxygen.trans3D.mul(data.tiles.craft.trans().to3D(tmpMat2)).translate(0f, 0f, data.tiles.craft.height())
        data.g3dview.each { tile: Tile ->
            val build = tile.build
            if (build != null && build is G3DrawBuilding) {
                cons(build)
            }
        }
        Oxygen.trans3D.set(tmpMat1)
    }

    fun g3dEach(cons: (G3DrawBuilding) -> Unit) {
        for (tiles in Vars.world.allTiles) {
            if (tiles.craft == null) continue
            g3dEach(getData(tiles), cons)
        }
    }

    fun draw3D() {
        g3dEach(G3DrawBuilding::draw3D)
    }

    fun drawBlocks(data: TilesRenderData) {
        data.apply {
            tmpMat1.set(OGraphics.trans3D())
            OGraphics.trans3D().translate(0f, 0f, tiles.craft.height() + 2f)
            tmpM.set(Draw.trans())
            Draw.trans(tiles.craft.trans())

            val pteam = Vars.player.team()

            //TODO
            //drawDestroyed()

            //draw most tile stuff
            for (i in 0..<tileview.size) {
                val tile = tileview.items[i]
                val block = tile.block()
                val build = tile.build

                Draw.z(Layer.block)

                val visible = (build == null || !build.inFogTo(pteam))

                //comment wasVisible part for hiding?
                if (block !== Blocks.air && (visible || build.wasVisible)) {
                    block.drawBase(tile)
                    Draw.reset()
                    Draw.z(Layer.block)

                    if (block.customShadow) {
                        Draw.z(Layer.block - 1)
                        block.drawShadow(tile)
                        Draw.z(Layer.block)
                    }

                    if (build != null) {
                        if (visible) {
                            build.visibleFlags = build.visibleFlags or (1L shl pteam.id)
                            if (!build.wasVisible) {
                                build.wasVisible = true
                                updateShadow(build)
                                Vars.renderer.minimap.update(tile)
                            }
                        }

                        if (build.damaged()) {
                            Draw.z(Layer.blockCracks)
                            build.drawCracks()
                            Draw.z(Layer.block)
                        }

                        if (build.team !== pteam) {
                            if (build.block.drawTeamOverlay) {
                                build.drawTeam()
                                Draw.z(Layer.block)
                            }
                        } else if (Vars.renderer.drawStatus && block.hasConsumers) {
                            build.drawStatus()
                        }
                    }
                    Draw.reset()
                } else if (!visible) {
                    //TODO here is the question: should buildings you lost sight of remain rendered? if so, how should this information be stored?
                    //uncomment lines below for buggy persistence
                    //if(build.wasVisible) updateShadow(build);
                    //build.wasVisible = false;
                }
            }

            if (Vars.renderer.lights.enabled()) {
                //draw lights
                for (i in 0..<lightview.size) {
                    val tile = lightview.items[i]
                    val entity = tile.build

                    if (entity != null) {
                        entity.drawLight()
                    } else if (tile.block().emitLight) {
                        tile.block().drawEnvironmentLight(tile)
                    } else if (tile.floor().emitLight && tile.block() === Blocks.air) { //only draw floor light under non-solid blocks
                        tile.floor().drawEnvironmentLight(tile)
                    }
                }
            }

            if (drawQuadtreeDebug) {
                //TODO remove
                Draw.z(Layer.overlayUI)
                Lines.stroke(1f, Color.green)

                blockTree.intersect(Core.camera.bounds(Tmp.r1)) { tile: Tile? ->
                    Lines.rect(tile!!.getHitbox(Tmp.r2))
                }

                Draw.reset()
            }

            OGraphics.trans3D(tmpMat1)
            Draw.trans(tmpM)
        }
    }

    /*
    fun eachDrawBlocks(func: Cons<Block>) {
        val pteam = Vars.player.team()

        //drawDestroyed()

        //draw most tile stuff
        for (i in 0..<tileview.size) {
            val tile = tileview.items[i]
            val block = tile.block()
            val build = tile.build

            Draw.z(Layer.block)

            val visible = (build == null || !build.inFogTo(pteam))

            //comment wasVisible part for hiding?
            if (block !== Blocks.air && (visible || build.wasVisible)) {
                func.get(block)
            }
        }
    }*/


    override fun updateShadow(build: Building) {
        if (build.tile == null) return
        if (build.tile.tiles.craft == null) return

        val data = getData(build.tile.tiles)
        val size = build.block.size
        val of = build.block.sizeOffset
        val tx = build.tile.x.toInt()
        val ty = build.tile.y.toInt()

        for (x in 0..<size) {
            for (y in 0..<size) {
                data.shadowEvents.add(data.tiles.get(x + tx + of, y + ty + of))
            }
        }
    }

    override fun updateShadowTile(tile: Tile) {
        if (tile.tiles.craft == null) return
        getData(tile.tiles).shadowEvents.add(tile)
    }

    override fun getCracks(building: Building?): Array<Array<TextureRegion?>?> {
        @Suppress("UNCHECKED_CAST")
        return cracks as Array<Array<TextureRegion?>?>
    }

    class BlockQuadtree(bounds: Rect) : QuadTree<Tile>(bounds) {
        public override fun hitbox(tile: Tile) {
            val block = tile.block()
            tmp.setCentered(tile.worldx() + block.offset, tile.worldy() + block.offset, block.clipSize, block.clipSize)
        }

        override fun newChild(rect: Rect): QuadTree<Tile> {
            return BlockQuadtree(rect)
        }
    }

    class BlockLightQuadtree(bounds: Rect) : QuadTree<Tile>(bounds) {
        public override fun hitbox(tile: Tile) {
            val block = tile.block()
            tmp.setCentered(
                tile.worldx() + block.offset,
                tile.worldy() + block.offset,
                block.lightClipSize,
                block.lightClipSize
            )
        }

        override fun newChild(rect: Rect): QuadTree<Tile> {
            return BlockLightQuadtree(rect)
        }
    }

    class Block3DQuadtree(bounds: Rect) : QuadTree<Tile>(bounds) {
        public override fun hitbox(tile: Tile) {
            val build = tile.build
            if (build is G3DrawBuilding) {
                build.draw3DHitbox(tmp, tile)
            } else {
                val block = tile.block()
                tmp.setCentered(
                    tile.worldx() + block.offset,
                    tile.worldy() + block.offset,
                    block.lightClipSize,
                    block.lightClipSize
                )
            }
        }

        override fun newChild(rect: Rect): QuadTree<Tile> {
            return Block3DQuadtree(rect)
        }
    }

    class FloorQuadtree(bounds: Rect) : QuadTree<Tile>(bounds) {
        public override fun hitbox(tile: Tile) {
            val floor = tile.floor()
            tmp.setCentered(tile.worldx(), tile.worldy(), floor.lightClipSize, floor.lightClipSize)
        }

        override fun newChild(rect: Rect): QuadTree<Tile> {
            return FloorQuadtree(rect)
        }
    }

}
