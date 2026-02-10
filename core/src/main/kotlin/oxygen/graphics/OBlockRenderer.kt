package oxygen.graphics

import arc.*
import arc.func.*
import arc.graphics.*
import arc.graphics.Texture.TextureFilter
import arc.graphics.g2d.*
import arc.graphics.gl.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.content.*
import mindustry.game.EventType.*
import mindustry.gen.*
import mindustry.graphics.*
import mindustry.world.*
import mindustry.world.blocks.environment.Floor.UpdateRenderState
import mindustry.world.blocks.power.*
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

    private val initialRequests: Int = 32 * 32

    lateinit var cracks: Array<Array<TextureRegion>>

    private val tileview = Seq<Tile>(false, initialRequests, Tile::class.java)
    private val g3dview = Seq<Tile>(false, initialRequests, Tile::class.java)
    private val lightview = Seq<Tile>(false, initialRequests, Tile::class.java)

    //TODO I don't like this system
    private val updateFloors = Seq<UpdateRenderState?>(UpdateRenderState::class.java)

    private var hadMapLimit = false
    private var lastCamX = 0
    private var lastCamY: Int = 0
    private var lastRangeX: Int = 0
    private var lastRangeY: Int = 0
    private var brokenFade = 0f
    private val shadows = FrameBuffer()
    private val dark = FrameBuffer()
    private val outArray2 = Seq<Building>()
    private val shadowEvents = Seq<Tile>()
    private val darkEvents = IntSet()
    private val procLinks = IntSet()
    private val procLights = IntSet()
    private val proc3D = IntSet()

    private var blockTree = BlockQuadtree(Rect(0f, 0f, 1f, 1f))
    private var blockLightTree = BlockLightQuadtree(Rect(0f, 0f, 1f, 1f))
    private var block3DTree = Block3DQuadtree(Rect(0f, 0f, 1f, 1f))
    private var floorTree = FloorQuadtree(Rect(0f, 0f, 1f, 1f))

    val floorL = OFloorRenderer()

    init {
        floor = floorL
        Events.on(ClientLoadEvent::class.java) { _: ClientLoadEvent ->
            cracks = Array(maxCrackSize) { size ->
                Array(crackRegions) { region ->
                    Core.atlas.find("cracks-${size + 1}-$region")!!
                }
            }
        }

        Events.on(WorldLoadEvent::class.java) { _: WorldLoadEvent ->
            reload()
        }

        //sometimes darkness gets disabled.
        Events.run(Trigger.newGame) {
            if (hadMapLimit && !Vars.state.rules.limitMapArea) {
                updateDarkness()
                Vars.renderer.minimap.updateAll()
            }
        }

        Events.on(TilePreChangeEvent::class.java) { event: TilePreChangeEvent ->
            //if (blockTree == null || floorTree == null) return@Cons
            if (indexBlock(event.tile)) {
                blockTree.remove(event.tile)
                blockLightTree.remove(event.tile)
                if (event.tile.build != null && event.tile.build is G3DrawBuilding) block3DTree.remove(event.tile)
            }
            if (indexFloor(event.tile)) floorTree.remove(event.tile)
        }

        Events.on(TileChangeEvent::class.java) { event: TileChangeEvent ->
            val visible = event!!.tile.build == null || !event.tile.build.inFogTo(Vars.player.team())
            if (event.tile.build != null) {
                event.tile.build.wasVisible = visible
            }

            if (visible) {
                shadowEvents.add(event.tile)
            }

            val avgx = (Core.camera.position.x / Vars.tilesize).toInt()
            val avgy = (Core.camera.position.y / Vars.tilesize).toInt()
            val rangex = (Core.camera.width / Vars.tilesize / 2).toInt() + 2
            val rangey = (Core.camera.height / Vars.tilesize / 2).toInt() + 2

            if (abs(avgx - event.tile.x) <= rangex && abs(avgy - event.tile.y) <= rangey) {
                lastCamX = -99
                lastCamY = lastCamX //invalidate camera position so blocks get updated
            }

            invalidateTile(event.tile)
            recordIndex(event.tile)
        }
    }

    override fun reload() {
        blockTree = BlockQuadtree(Rect(0f, 0f, Vars.world.unitWidth().toFloat(), Vars.world.unitHeight().toFloat()))
        blockLightTree =
            BlockLightQuadtree(Rect(0f, 0f, Vars.world.unitWidth().toFloat(), Vars.world.unitHeight().toFloat()))
        block3DTree =
            Block3DQuadtree(Rect(0f, 0f, Vars.world.unitWidth().toFloat(), Vars.world.unitHeight().toFloat()))

        floorTree = FloorQuadtree(Rect(0f, 0f, Vars.world.unitWidth().toFloat(), Vars.world.unitHeight().toFloat()))

        shadowEvents.clear()
        updateFloors.clear()
        lastCamX = -99
        lastCamY = lastCamX //invalidate camera position so blocks get updated
        hadMapLimit = Vars.state.rules.limitMapArea

        shadows.texture.setFilter(TextureFilter.linear, TextureFilter.linear)
        shadows.resize(Vars.world.width(), Vars.world.height())
        shadows.begin()
        Core.graphics.clear(Color.white)
        Draw.proj().setOrtho(0f, 0f, shadows.width.toFloat(), shadows.height.toFloat())

        Draw.color(blendShadowColor)

        for (tile in Vars.world.tiles) {
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
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
            }
        }

        Draw.flush()
        Draw.color()
        shadows.end()

        updateDarkness()
    }

    override fun updateShadows(ignoreBuildings: Boolean, ignoreTerrain: Boolean) {
        shadows.texture.setFilter(TextureFilter.linear, TextureFilter.linear)
        shadows.resize(Vars.world.width(), Vars.world.height())
        shadows.begin()
        Core.graphics.clear(Color.white)
        Draw.proj().setOrtho(0f, 0f, shadows.width.toFloat(), shadows.height.toFloat())

        Draw.color(blendShadowColor)

        for (tile in Vars.world.tiles) {
            if (tile.block()
                    .displayShadow(tile) && (tile.build == null || tile.build.wasVisible) && !(ignoreBuildings && !tile.block().isStatic) && !(ignoreTerrain && tile.block().isStatic)
            ) {
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
            }
        }

        Draw.flush()
        Draw.color()
        shadows.end()
    }

    override fun updateDarkness() {
        darkEvents.clear()
        dark.texture.setFilter(TextureFilter.linear)
        dark.resize(Vars.world.width(), Vars.world.height())
        dark.begin()

        //fill darkness with black when map area is limited
        Core.graphics.clear(if (Vars.state.rules.limitMapArea) Color.black else Color.white)
        Draw.proj().setOrtho(0f, 0f, dark.width.toFloat(), dark.height.toFloat())

        //clear out initial starting area
        if (Vars.state.rules.limitMapArea) {
            Draw.color(Color.white)
            Fill.crect(
                Vars.state.rules.limitX.toFloat(),
                Vars.state.rules.limitY.toFloat(),
                Vars.state.rules.limitWidth.toFloat(),
                Vars.state.rules.limitHeight.toFloat()
            )
        }

        for (tile in Vars.world.tiles) {
            //skip lighting outside rect
            if (Vars.state.rules.limitMapArea && !Rect.contains(
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

            val darkness = Vars.world.getDarkness(tile.x.toInt(), tile.y.toInt())

            if (darkness > 0) {
                val dark = 1f - min((darkness + 0.5f) / 4f, 1f)
                Draw.colorl(dark)
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
            }
        }

        Draw.flush()
        Draw.color()
        dark.end()
    }

    override fun invalidateTile(tile: Tile) {
        val avgx = (Core.camera.position.x / Vars.tilesize).toInt()
        val avgy = (Core.camera.position.y / Vars.tilesize).toInt()
        val rangex = (Core.camera.width / Vars.tilesize / 2).toInt() + 3
        val rangey = (Core.camera.height / Vars.tilesize / 2).toInt() + 3

        if (abs(avgx - tile.x) <= rangex && abs(avgy - tile.y) <= rangey) {
            lastCamX = -99
            lastCamY = lastCamX //invalidate camera position so blocks get updated
        }
    }

    override fun getShadowBuffer(): FrameBuffer {
        return shadows
    }

    override fun removeFloorIndex(tile: Tile) {
        if (indexFloor(tile)) floorTree.remove(tile)
    }

    override fun addFloorIndex(tile: Tile) {
        if (indexFloor(tile)) floorTree.insert(tile)
    }

    fun indexBlock(tile: Tile): Boolean {
        val block = tile.block()
        return tile.isCenter && block !== Blocks.air && block.cacheLayer === CacheLayer.normal
    }

    fun indexFloor(tile: Tile): Boolean {
        return tile.block() === Blocks.air && tile.floor().emitLight && Vars.world.getDarkness(
            tile.x.toInt(),
            tile.y.toInt()
        ) < 3
    }

    fun recordIndex(tile: Tile) {
        if (indexBlock(tile)) {
            blockTree.insert(tile)
            blockLightTree.insert(tile)
            if (tile.build is G3DrawBuilding) block3DTree.insert(tile)
        }
        if (indexFloor(tile)) floorTree.insert(tile)
    }

    override fun recacheWall(tile: Tile) {
        for (cx in tile.x - Vars.darkRadius..tile.x + Vars.darkRadius) {
            for (cy in tile.y - Vars.darkRadius..tile.y + Vars.darkRadius) {
                val other = Vars.world.tile(cx, cy)
                if (other != null) {
                    darkEvents.add(other.pos())
                    floor.recacheTile(other)
                }
            }
        }
    }

    fun checkChanges() {
        darkEvents.each { pos: Int ->
            val tile = Vars.world.tile(pos)
            if (tile != null && tile.block().fillsTile) {
                tile.data = Vars.world.getWallDarkness(tile)
            }
        }
    }

    fun processDarkness() {
        if (!darkEvents.isEmpty) {
            Draw.flush()

            dark.begin()
            Draw.proj().setOrtho(0f, 0f, dark.width.toFloat(), dark.height.toFloat())

            darkEvents.each(Intc { pos: Int ->
                val tile = Vars.world.tile(pos) ?: return@Intc
                val darkness = Vars.world.getDarkness(tile.x.toInt(), tile.y.toInt())
                //then draw the shadow
                Draw.colorl(if (darkness <= 0f) 1f else 1f - min((darkness + 0.5f) / 4f, 1f))
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
            })

            Draw.flush()
            Draw.color()
            dark.end()
            darkEvents.clear()

            Draw.proj(Core.camera)
        }
    }

    fun drawDarkness() {
        Draw.shader(OCShaders.darkness)
        Draw.fbo(dark.texture, Vars.world.width(), Vars.world.height(), Vars.tilesize, Vars.tilesize / 2f)
        Draw.shader()
    }

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
    }

    override fun processShadows() {
        if (!shadowEvents.isEmpty) {
            Draw.flush()

            shadows.begin()
            Draw.proj().setOrtho(0f, 0f, shadows.width.toFloat(), shadows.height.toFloat())

            for (tile in shadowEvents) {
                if (tile == null) continue
                //draw white/shadow color depending on blend
                Draw.color(
                    if (!tile.block()
                            .displayShadow(tile) || (Vars.state.rules.fog && tile.build != null && !tile.build.wasVisible)
                    ) Color.white else blendShadowColor
                )
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1f, 1f)
            }

            Draw.flush()
            Draw.color()
            shadows.end()
            shadowEvents.clear()

            Draw.proj(Core.camera)
        }
    }

    fun drawShadows() {
        // processShadows()
        val ww = (Vars.world.width() * Vars.tilesize).toFloat()
        val wh = (Vars.world.height() * Vars.tilesize).toFloat()
        val x = Core.camera.position.x + Vars.tilesize / 2f
        val y = Core.camera.position.y + Vars.tilesize / 2f
        val u = (x - Core.camera.width / 2f) / ww
        val v = (y - Core.camera.height / 2f) / wh
        val u2 = (x + Core.camera.width / 2f) / ww
        val v2 = (y + Core.camera.height / 2f) / wh

        Tmp.tr1.set(shadows.texture)
        Tmp.tr1.set(u, v2, u2, v)

        Draw.shader(OCShaders.darkness)
        Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height)
        Draw.shader()
    }

    /** Process all blocks to draw.  */
    fun processBlocks() {
        val avgx = (Core.camera.position.x / Vars.tilesize).toInt()
        val avgy = (Core.camera.position.y / Vars.tilesize).toInt()

        val rangex = (Core.camera.width / Vars.tilesize / 2).toInt()
        val rangey = (Core.camera.height / Vars.tilesize / 2).toInt()

        if (!Vars.state.isPaused) {
            val updates = updateFloors.size
            val uitems = updateFloors.items
            for (i in 0..<updates) {
                val tile: UpdateRenderState = uitems[i]!!
                tile.floor.renderUpdate(tile)
            }
        }


        if (avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey) {
            return
        }

        tileview.clear()
        lightview.clear()
        g3dview.clear()
        procLinks.clear()
        procLights.clear()
        proc3D.clear()

        val bounds = Core.camera.bounds(Tmp.r3).grow(Vars.tilesize * 2f)

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

        lastCamX = avgx
        lastCamY = avgy
        lastRangeX = rangex
        lastRangeY = rangey
    }

    //debug method for drawing block bounds
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
    }

    fun g3dEach(cons: (G3DrawBuilding) -> Unit) {
        g3dview.each { tile: Tile ->
            val build = tile.build
            if (build != null && build is G3DrawBuilding) {
                cons(build)
            }
        }
    }

    fun draw3DDepth() {
        g3dEach(G3DrawBuilding::drawDepth)
    }

    fun draw3D() {
        g3dEach(G3DrawBuilding::draw3D)
    }

    fun drawBlocks() {
        val pteam = Vars.player.team()

        drawDestroyed()

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
    }

    fun eachDrawBlocks(func: Cons<Block>) {
        val pteam = Vars.player.team()

        drawDestroyed()

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
    }


    override fun updateShadow(build: Building) {
        if (build.tile == null) return
        val size = build.block.size
        val of = build.block.sizeOffset
        val tx = build.tile.x.toInt()
        val ty = build.tile.y.toInt()

        for (x in 0..<size) {
            for (y in 0..<size) {
                shadowEvents.add(Vars.world.tile(x + tx + of, y + ty + of))
            }
        }
    }

    override fun updateShadowTile(tile: Tile?) {
        shadowEvents.add(tile)
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
