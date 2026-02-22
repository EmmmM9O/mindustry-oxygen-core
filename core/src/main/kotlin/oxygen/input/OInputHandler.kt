package oxygen.input

import arc.*
import arc.func.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.input.*
import arc.math.*
import arc.math.geom.*
import arc.scene.*
import arc.scene.event.*
import arc.scene.ui.layout.*
import arc.struct.*
import arc.struct.Queue
import arc.util.*
import mindustry.*
import mindustry.Vars.*
import mindustry.ai.*
import mindustry.ai.types.*
import mindustry.content.*
import mindustry.core.*
import mindustry.entities.*
import mindustry.entities.units.*
import mindustry.game.*
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.graphics.*
import mindustry.input.*
import mindustry.ui.*
import mindustry.world.*
import mindustry.world.blocks.*
import mindustry.world.blocks.ConstructBlock.ConstructBuild
import mindustry.world.blocks.distribution.*
import oxygen.*
import oxygen.math.geom.*
import java.util.*
import kotlin.*
import kotlin.collections.*
import kotlin.math.*
import kotlin.ranges.*

abstract class OInputHandler : InputHandler() {
    val v1 = Vec2()
    val posTiles: Seq<Tile> = Seq.with()
    var currentTiles: Tiles? = null
    var currentTile: Tile? = null
    val tilesPlans: ObjectMap<Tiles, Queue<BuildPlan>> = ObjectMap.of()

    fun genTilesPlans(plans: Seq<BuildPlan>): ObjectMap<Tiles, Queue<BuildPlan>> {
        tilesPlans.clear();
        for (plan in plans) {
            tilesPlans.get(plan.tiles, ::Queue).add(plan);
        }
        return tilesPlans
    }

    /*Get tiles according to world positio*/
    fun getTile(x: Float, y: Float): Tile? {
        posTiles.clear()
        world.tilesScreenTree.contains(x, y) { tiles ->
            Oxygen.renderer.unporjectTiles(tiles, v1.set(x, y))
            if (selectedBlock()) {
                v1.sub(block.offset, block.offset)
            }
            val tile = tiles.tileWorld(v1.x, v1.y)
            if (tile != null) posTiles.add(tile)
        }
        if (posTiles.size == 0) return null
        posTiles.sort { it.tiles.realHeight() }
        return posTiles.peek()
    }

    fun rawTilePos(): Vec2 {
        return rawTilePos(currentTiles!!)
    }

    fun rawTilePos(tiles: Tiles): Vec2 {
        return getTilePos(tiles, Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
    }

    fun getPos(tiles: Tiles, x: Float, y: Float): Vec2 {
        val vec = Core.input.mouseWorld(x, y)
        Oxygen.renderer.unporjectTiles(tiles, vec)
        return vec
    }

    fun getTilePos(tiles: Tiles, x: Float, y: Float): Vec2 {
        val vec = getPos(tiles, x, y)
        if (selectedBlock()) {
            vec.sub(block.offset, block.offset)
        }
        return vec.set(World.toTile(vec.x).toFloat(), World.toTile(vec.y).toFloat())
    }

    override fun tileAtF(x: Float, y: Float): Tile? {
        val vec = Core.input.mouseWorld(x, y)
        return getTile(vec.x, vec.y)
    }

    override fun tileAt(x: Float, y: Float): Tile? {
        val vec = Core.input.mouseWorld(x, y)
        val tile = getTile(vec.x, vec.y)
        if (tile == null) {
            currentTiles = null
            currentTile = null
            return null
        }
        currentTiles = tile.tiles
        currentTile = tile
        return tile
    }

    fun validPlace(
        x: Int,
        y: Int,
        tiles: Tiles,
        type: Block,
        rotation: Int,
        ignore: BuildPlan? = null,
        ignoreUnits: Boolean = false
    ): Boolean {
        if (player.isBuilder() && player.unit().plans.size > 0) {
            Tmp.r1.setCentered(
                x * tilesize + type.offset,
                y * tilesize + type.offset,
                type.size.toFloat() * tilesize,
                type.size.toFloat() * tilesize
            )
            plansOut.clear()
            playerPlanTree.intersect(Tmp.r1, plansOut)

            for (plan in plansOut) {
                if (plan.tiles == tiles &&
                    plan != ignore &&
                    !plan.breaking &&
                    plan.block.bounds(plan.x, plan.y, Tmp.r1).overlaps(type.bounds(x, y, Tmp.r2)) &&
                    !(type.canReplace(plan.block) && Tmp.r1.equals(Tmp.r2))
                ) {
                    return false
                }
            }
        }
        return if (ignoreUnits) Build.validPlaceIgnoreUnits(
            type,
            player.team(),
            tiles,
            x,
            y,
            rotation,
            true,
            true
        ) else Build.validPlace(type, player.team(), tiles, x, y, rotation)
    }

    fun validBreak(x: Int, y: Int, tiles: Tiles): Boolean {
        return Build.validBreak(player.team(), tiles, x, y)
    }

    /** @return the selection plan that overlaps this position, or null. */
    fun getPlan(x: Int, y: Int, tiles: Tiles): BuildPlan? {
        return getPlan(x, y, tiles, 1, null)
    }

    /** @return the selection plan that overlaps this position, or null. */
    fun getPlan(x: Int, y: Int, tiles: Tiles, size: Int, skip: BuildPlan? = null): BuildPlan? {
        val offset = ((size + 1) % 2) * tilesize / 2f
        InputHandler.r2.setSize(tilesize * size.toFloat())
        InputHandler.r2.setCenter(x * tilesize + offset, y * tilesize + offset)

        resultplan = null
        val test = Boolf(fun(plan: BuildPlan): Boolean {
            if (plan.tiles != tiles) return false
            if (plan == skip) return false
            val other = plan.tile() ?: return false

            if (!plan.breaking) {
                InputHandler.r1.setSize(plan.block.size.toFloat() * tilesize)
                InputHandler.r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset)
            } else {
                InputHandler.r1.setSize(other.block().size.toFloat() * tilesize)
                InputHandler.r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset)
            }

            return InputHandler.r2.overlaps(InputHandler.r1)
        })
        if (!player.dead()) {
            for (plan in player.unit().plans()) {
                if (test.get(plan)) return plan
            }
        }

        return selectPlans.find(test)
    }

    override protected fun flushSelectPlans(plans: Seq<BuildPlan>) {
        for (plan in plans) {
            if (plan.block != null && validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation, null, true)) {
                val other = getPlan(plan.x, plan.y, plan.tiles, plan.block.size, null)
                if (other == null) {
                    selectPlans.add(plan.copy())
                } else if (!other.breaking && other.x == plan.x && other.y == plan.y && other.block.size == plan.block.size) {
                    selectPlans.remove(other)
                    selectPlans.add(plan.copy())
                }
            }
        }
    }

    override protected fun flushPlansReverse(plans: Seq<BuildPlan>) {
        //reversed iteration.
        for (i in plans.size - 1 downTo 0) {
            val plan = plans.get(i)
            if (plan.block != null && validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation, null, true)) {
                val copy = plan.copy()
                plan.block.onNewPlan(copy)
                player.unit().addBuild(copy, false)
            }
        }
    }

    override protected fun flushPlans(plans: Seq<BuildPlan>) {
        for (plan in plans) {
            if (plan.block != null && validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation, null, true)) {
                val copy = plan.copy()
                plan.block.onNewPlan(copy)
                player.unit().addBuild(copy)
            }
        }
    }


    override protected fun drawOverPlan(plan: BuildPlan) {
        drawOverPlan(plan, validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation))
    }

    override protected fun drawOverPlan(plan: BuildPlan, valid: Boolean) {
        Draw.reset()
        Draw.mixcol(
            if (!valid) Pal.breakInvalid else Color.white,
            (if (!valid) 0.4f else 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f)
        )
        Draw.alpha(1f)
        plan.block.drawPlanConfigTop(plan, allSelectLines)
        Draw.reset()
    }

    override protected open fun drawPlan(plan: BuildPlan) {
        drawPlan(plan, validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation).also { plan.cachedValid = it })
    }

    override protected fun drawPlan(plan: BuildPlan, valid: Boolean) {
        plan.block.drawPlan(plan, allPlans(), valid)
    }

    /** Draws a placement icon for a specific block.  */
    protected fun drawPlan(x: Int, y: Int, tiles: Tiles, block: Block, rotation: Int) {
        bplan.set(x, y, rotation, block)
        bplan.tiles = tiles
        if (block.saveConfig) {
            bplan.config = block.lastConfig
        }
        bplan.animScale = 1f
        block.drawPlan(bplan, allPlans(), validPlace(x, y, tiles, block, rotation))
    }

    override fun planMatches(plan: BuildPlan): Boolean {
        val tile = plan.tiles.tile(plan.x, plan.y)
        return tile != null && tile.build is ConstructBuild && (tile.build as ConstructBuild).current === plan.block
    }

    fun drawBreaking(x: Int, y: Int, tiles: Tiles) {
        val tile = tiles.tile(x, y)
        if (tile == null) return
        val block = tile.block()

        drawSelected(x, y, block, Pal.remove)
    }

    fun drawSelected(x: Int, y: Int, tiles: Tiles, block: Block?, color: Color) {
        Drawf.selected(x, y, block, color)
    }

    override fun drawBreaking(plan: BuildPlan) {
        if (plan.breaking) {
            drawBreaking(plan.x, plan.y, plan.tiles)
        } else {
            drawSelected(plan.x, plan.y, plan.tiles, plan.block, Pal.remove)
        }
    }

    fun breakBlock(x: Int, y: Int, tiles: Tiles) {
        if (!player.isBuilder()) return

        var tile = tiles.tile(x, y)
        if (tile != null && tile.build != null) tile = tile.build.tile!!
        player.unit().addBuild(BuildPlan(tile.x.toInt(), tile.y.toInt(), tiles))
    }

    fun tryBreakBlock(x: Int, y: Int, tiles: Tiles) {
        if (validBreak(x, y, tiles)) {
            breakBlock(x, y, tiles)
        }
    }

    /** Remove everything from the queue in a selection.  */
    override protected fun removeSelection(x1: Int, y1: Int, x2: Int, y2: Int, flush: Boolean, maxLength: Int) {
        val tiles = lineTiles
        val result = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength)
        for (x in 0..abs(result.x2 - result.x)) {
            for (y in 0..abs(result.y2 - result.y)) {
                val wx = x1 + x * Mathf.sign((x2 - x1).toFloat())
                val wy = y1 + y * Mathf.sign((y2 - y1).toFloat())

                val tile = tiles.tileBuilding(wx, wy)

                if (tile == null) continue

                if (!flush) {
                    tryBreakBlock(wx, wy, tiles)
                } else if (validBreak(
                        tile.x.toInt(),
                        tile.y.toInt(),
                        tiles
                    ) && !selectPlans.contains { r -> r.tile() != null && r.tile() === tile }
                ) {
                    selectPlans.add(BuildPlan(tile.x.toInt(), tile.y.toInt(), tiles))
                }
            }
        }

        //remove build plans
        Tmp.r1.set(
            (result.x * tilesize).toFloat(),
            (result.y * tilesize).toFloat(),
            ((result.x2 - result.x) * tilesize).toFloat(),
            ((result.y2 - result.y) * tilesize).toFloat()
        )

        if (!player.dead()) {
            var it = player.unit().plans().iterator()
            while (it.hasNext()) {
                val plan = it.next()
                if (!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)) {
                    it.remove()
                }
            }

            //don't remove plans on desktop, where flushing is false
            if (flush) {
                it = selectPlans.iterator()
                while (it.hasNext()) {
                    val plan = it.next()
                    if (!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)) {
                        it.remove()
                    }
                }
            }
        }

        InputHandler.removed.clear()

        //remove blocks to rebuild
        val broken = player.team().data().plans.iterator()
        while (broken.hasNext()) {
            val plan = broken.next()
            val block = plan.block
            if (plan.tiles == tiles && block.bounds(plan.x.toInt(), plan.y.toInt(), Tmp.r2).overlaps(Tmp.r1)) {
                InputHandler.removed.add(Point2.pack(plan.x.toInt(), plan.y.toInt()))
                plan.removed = true
                broken.remove()
            }
        }

        //TODO array may be too large?
        if (InputHandler.removed.size > 0 && net.active()) {
            Call.deletePlans(player, tiles, InputHandler.removed.toArray())
        }
    }

    override fun rebuildArea(x1: Int, y1: Int, x2: Int, y2: Int) {
        val tiles = lineTiles
        val result = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, 999999999)
        Tmp.r1.set(
            (result.x * tilesize).toFloat(),
            (result.y * tilesize).toFloat(),
            ((result.x2 - result.x) * tilesize).toFloat(),
            ((result.y2 - result.y) * tilesize).toFloat()
        )

        val broken = player.team().data().plans.iterator()
        while (broken.hasNext()) {
            val plan = broken.next()
            val block = plan.block
            if (plan.tiles == tiles && block.bounds(plan.x.toInt(), plan.y.toInt(), Tmp.r2).overlaps(Tmp.r1)) {
                player.unit()
                    .addBuild(
                        BuildPlan(
                            plan.x.toInt(),
                            plan.y.toInt(),
                            plan.tiles,
                            plan.rotation.toInt(),
                            plan.block,
                            plan.config
                        )
                    )
            }
        }

        InputHandler.intSet.clear()
        for (x in result.x..result.x2) {
            for (y in result.y..result.y2) {
                val tile = tiles.tileBuilding(x, y)

                if (tile != null && tile.build != null && InputHandler.intSet.add(tile.pos())) {
                    tryRepairDerelict(tile)
                }
            }
        }
    }

    override fun tryRepairDerelict(selected: Tile?): Boolean {
        if (!player.dead() && selected != null && !state.rules.editor && player.team() !== Team.derelict && selected.build != null && selected.build.block.unlockedNow() && selected.build.team === Team.derelict &&
            Build.validPlace(
                selected.block(),
                player.team(),
                selected.tiles,
                selected.build.tileX(),
                selected.build.tileY(),
                selected.build.rotation
            )
        ) {
            player.unit().addBuild(
                BuildPlan(
                    selected.build.tileX(),
                    selected.build.tileY(),
                    selected.tiles,
                    selected.build.rotation,
                    selected.block(),
                    selected.build.config()
                )
            )
            return true
        }
        return false
    }

    override protected fun updateLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        val tiles = lineTiles
        linePlans.clear()
        iterateLine(tiles, x1, y1, x2, y2) { l ->
            rotation = l.rotation
            val plan = BuildPlan(l.x, l.y, tiles, l.rotation, block, block.nextConfig())
            plan.animScale = 1f
            linePlans.add(plan)
        }

        if (Core.settings.getBool("blockreplace")) {
            linePlans.each { plan ->
                val replace = plan.block.getReplacement(plan, linePlans)
                if (replace.unlockedNow()) {
                    plan.block = replace
                }
            }

            block.handlePlacementLine(linePlans)
        }
    }

    override protected fun updateLine(x1: Int, y1: Int) {
        val (lx, ly) = rawTilePos(lineTiles)
        updateLine(x1, y1, lx.toInt(), ly.toInt())
    }

    fun iterateLine(tiles: Tiles, startX: Int, startY: Int, endX: Int, endY: Int, cons: Cons<PlaceLine>) {
        val points: Seq<Point2>
        var diagonal = Core.input.keyDown(Binding.diagonalPlacement)

        if (Core.settings.getBool("swapdiagonal") && mobile) {
            diagonal = !diagonal
        }

        if (block != null && block.swapDiagonalPlacement) {
            diagonal = !diagonal
        }

        var endRotation = -1
        val start = tiles.build(startX, startY)
        val end = tiles.build(endX, endY)
        if (diagonal && (block == null || block.allowDiagonal)) {
            if (block != null && start is ChainedBuilding && end is ChainedBuilding
                && block.canReplace(end.block) && block.canReplace(start.block)
            ) {
                points = Placement.upgradeLine(tiles, startX, startY, endX, endY)
            } else {
                points =
                    Placement.pathfindLine(tiles, block != null && block.conveyorPlacement, startX, startY, endX, endY)
            }
        } else if (block != null && block.allowRectanglePlacement) {
            points = Placement.normalizeRectangle(startX, startY, endX, endY, block.size)
        } else {
            points = Placement.normalizeLine(startX, startY, endX, endY)
        }
        if (points.size > 1 && end is ChainedBuilding) {
            val secondToLast = points.get(points.size - 2)
            if (tiles.build(secondToLast.x, secondToLast.y) !is ChainedBuilding) {
                endRotation = end.rotation
            }
        }

        if (block != null) {
            block.changePlacementPath(points, rotation, diagonal)
        }

        val angle = Angles.angle(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())
        var baseRotation = rotation
        if (!overrideLineRotation || diagonal) {
            baseRotation = if (startX == endX && startY == endY) rotation else (((angle + 45) / 90f).toInt()) % 4
        }

        Tmp.r3.set(-1f, -1f, 0f, 0f)

        for (i in 0..<points.size) {
            val point = points.get(i)

            if (block != null && Tmp.r2.setSize((block.size * tilesize).toFloat())
                    .setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset).overlaps(Tmp.r3)
            ) {
                continue
            }

            val next = if (i == points.size - 1) null else points.get(i + 1)
            line.x = point.x
            line.y = point.y
            if ((!overrideLineRotation || diagonal) && !(block != null && block.ignoreLineRotation)) {
                var result = baseRotation
                if (next != null) {
                    result = Tile.relativeTo(point.x, point.y, next.x, next.y).toInt()
                } else if (endRotation != -1) {
                    result = endRotation
                } else if (block.conveyorPlacement && i > 0) {
                    val prev = points.get(i - 1)
                    result = Tile.relativeTo(prev.x, prev.y, point.x, point.y).toInt()
                }
                if (result != -1) {
                    line.rotation = result
                }
            } else {
                line.rotation = rotation
            }
            line.last = next == null
            cons.get(line)

            Tmp.r3.setSize((block.size * tilesize).toFloat())
                .setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset)
        }
    }

    override fun checkConfigTap(): Boolean {
        val (x, y) = rawTilePos()
        return config.isShown() && config.getSelected()
            .onConfigureTapped(x, y)
    }

    override fun selectedControlBuild(): Building? {
        val build = tileAtF(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())?.build
        if (build != null && !player.dead() && build.canControlSelect(player.unit()) && build.team === player.team()) {
            return build
        }
        return null
    }

    override fun selectedUnit(): Unit? {
        val unit = Units.closest(
            player.team(),
            Core.input.mouseWorld().x,
            Core.input.mouseWorld().y,
            40f
        )
        { u -> u.isAI() && u.playerControllable() }
        if (unit != null) {
            unit.hitbox(Tmp.r1)
            Tmp.r1.grow(6f)
            if (Tmp.r1.contains(Core.input.mouseWorld())) {
                return unit
            }
        }

        val build = tileAtF(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())?.build
        if (build != null && build is ControlBlock && build.canControl() && build.team === player.team() && build.unit() !== player.unit() && build.unit()
                .isAI()
        ) {
            return build.unit()
        }

        return null
    }


    /*
    fun addLock(lock: Boolp?) {
        inputLocks.add(lock)
    }

    fun locked(): Boolean {
        return inputLocks.contains(Boolf { obj: Boolp? -> obj!!.get() })
    }

    fun allPlans(): Eachable<BuildPlan?> {
        return allPlans
    }

    fun isUsingSchematic(): Boolean {
        return !selectPlans.isEmpty()
    }

    fun spectate(unit: Unit?) {
        spectating = unit
        Core.camera.position.set(unit)
    }

    open fun update() {
        if (spectating != null && (!spectating.isValid() || spectating.team !== player.team())) {
            spectating = null
        }

        if (logicCutscene && !renderer.isCutscene()) {
            Core.camera.position.lerpDelta(logicCamPan, logicCamSpeed)
        } else {
            logicCutsceneZoom = -1f
        }

        itemDepositCooldown -= Time.delta / 60f

        commandBuildings.removeAll(Boolf { b: Building? -> !b!!.isValid() || !b.isCommandable() || b.team !== player.team() })

        if (!commandMode) {
            commandRect = false
        }

        if (player.isBuilder()) {
            val playerPlans = player.unit().plans
            if (player.unit() !== lastUnit && playerPlans.size <= 1) {
                playerPlans.ensureCapacity(lastPlans.size)
                for (plan in lastPlans) {
                    playerPlans.addLast(plan)
                }
            }
            if (lastPlans.size != playerPlans.size || (lastPlans.size > 0 && playerPlans.size > 0 && lastPlans.first() !== playerPlans.first())) {
                lastPlans.clear()
                for (plan in playerPlans) {
                    lastPlans.addLast(plan)
                }
            }
        }

        lastUnit = player.unit()

        playerPlanTree.clear()
        if (!player.dead()) {
            player.unit().plans.each(Cons { obj: BuildPlan? -> playerPlanTree.insert(obj) })
        }

        player.typing = ui.chatfrag.shown()

        if (player.dead()) {
            droppingItem = false
        }

        if (player.isBuilder()) {
            player.unit().updateBuilding(isBuilding)
        }

        if (!player.dead() && player.shooting && !wasShooting && player.unit()
                .hasWeapons() && state.rules.unitAmmo && !player.team().rules().infiniteAmmo && player.unit().ammo <= 0
        ) {
            player.unit().type.weapons.first().noAmmoSound.at(player.unit())
        }

        //you don't want selected blocks while locked, looks weird
        if (locked()) {
            block = null
        }

        wasShooting = player.shooting

        //only reset the controlled type and control a unit after the timer runs out
        //essentially, this means the client waits for ~1 second after controlling something before trying to control something else automatically
        if (!player.dead() && (Time.delta / 70f.let { recentRespawnTimer -= it; recentRespawnTimer }) <= 0f && player.justSwitchFrom !== player.unit()) {
            controlledType = player.unit().type
        }

        if (controlledType != null && player.dead() && controlledType.playerControllable) {
            val unit = Units.closest(
                player.team(),
                player.x,
                player.y,
                Boolf { u: Unit? -> !u!!.isPlayer() && u.type === controlledType && u.playerControllable() && !u.dead })

            if (unit != null) {
                //only trying controlling once a second to prevent packet spam
                if (!net.client() || controlInterval.get(0, 70f)) {
                    recentRespawnTimer = 1f
                    Call.unitControl(player, unit)
                }
            }
        }
    }

    fun checkUnit() {
        if (controlledType != null && controlledType.playerControllable) {
            var unit = Units.closest(
                player.team(),
                player.x,
                player.y,
                Boolf { u: Unit? -> !u!!.isPlayer() && u.type === controlledType && !u.dead })
            if (unit == null && controlledType === UnitTypes.block) {
                unit =
                    if (world.buildWorld(player.x, player.y) is ControlBlock && cont.canControl()) cont.unit() else null
            }

            if (unit != null) {
                if (net.client()) {
                    Call.unitControl(player, unit)
                } else {
                    unit.controller(player)
                }
            }
        }
    }

    fun tryPickupPayload() {
        val unit = player.unit()
        if (unit !is Payloadc) return

        val target = Units.closest(
            player.team(),
            unit.x(),
            unit.y(),
            unit.type.hitSize * 2f,
            Boolf { u: Unit? ->
                u!!.isAI() && u.isGrounded() && unit.canPickup(u) && u.within(
                    unit,
                    u.hitSize + unit.hitSize
                )
            })
        if (target != null) {
            Call.requestUnitPayload(player, target)
        } else {
            val build = world.buildWorld(unit.x(), unit.y())

            if (build != null && state.teams.canInteract(unit.team, build.team)) {
                Call.requestBuildPayload(player, build)
            }
        }
    }

    fun tryDropPayload() {
        val unit = player.unit()
        if (unit !is Payloadc) return

        Call.requestDropPayload(player, player.x, player.y)
    }

    open fun getMouseX(): Float {
        return Core.input.mouseX().toFloat()
    }

    open fun getMouseY(): Float {
        return Core.input.mouseY().toFloat()
    }

    open fun buildPlacementUI(table: Table?) {
    }

    open fun buildUI(group: Group?) {
    }

    open fun updateState() {
        if (state.isMenu()) {
            controlledType = null
            logicCutscene = false
            config.forceHide()
            commandRect = false
            commandMode = commandRect
        }
    }

    //TODO when shift is held? ctrl?
    fun multiUnitSelect(): Boolean {
        return false
    }

    fun selectUnitsRect() {
        if (commandMode && commandRect) {
            if (!tappedOne) {
                val units = selectedCommandUnits(
                    commandRectX,
                    commandRectY,
                    Core.input.mouseWorldX() - commandRectX,
                    Core.input.mouseWorldY() - commandRectY
                )
                if (multiUnitSelect()) {
                    //tiny brain method of unique addition
                    selectedUnits.removeAll(units)
                } else {
                    //nothing selected, clear units
                    selectedUnits.clear()
                }
                commandBuildings.clear()

                selectedUnits.addAll(units)
                if (selectedUnits.isEmpty()) {
                    commandBuildings.addAll(
                        selectedCommandBuildings(
                            commandRectX,
                            commandRectY,
                            Core.input.mouseWorldX() - commandRectX,
                            Core.input.mouseWorldY() - commandRectY
                        )
                    )
                }
                Events.fire<EventType.Trigger?>(EventType.Trigger.unitCommandChange)
            }
            commandRect = false
        }
    }

    fun selectTypedUnits() {
        if (commandMode) {
            val unit = selectedCommandUnit(Core.input.mouseWorldX(), Core.input.mouseWorldY())
            if (unit != null) {
                selectedUnits.clear()
                Core.camera.bounds(Tmp.r1)
                selectedUnits.addAll(
                    selectedCommandUnits(
                        Tmp.r1.x,
                        Tmp.r1.y,
                        Tmp.r1.width,
                        Tmp.r1.height,
                        Boolf { u: Unit? -> u!!.type === unit.type })
                )
                Events.fire<EventType.Trigger?>(EventType.Trigger.unitCommandChange)
            }
        }
    }

    fun tapCommandUnit() {
        if (commandMode) {
            val unit = selectedCommandUnit(Core.input.mouseWorldX(), Core.input.mouseWorldY())
            val build = world.buildWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY())
            if (unit != null) {
                if (!selectedUnits.contains(unit)) {
                    selectedUnits.add(unit)
                } else {
                    selectedUnits.remove(unit)
                }
                commandBuildings.clear()
            } else {
                //deselect
                selectedUnits.clear()

                if (build != null && build.team === player.team() && build.isCommandable()) {
                    if (commandBuildings.contains(build)) {
                        commandBuildings.remove(build)
                    } else {
                        commandBuildings.add(build)
                    }
                } else {
                    commandBuildings.clear()
                }
            }
            Events.fire<EventType.Trigger?>(EventType.Trigger.unitCommandChange)
        }
    }

    fun commandTap(screenX: Float, screenY: Float) {
        commandTap(screenX, screenY, false)
    }

    fun commandTap(screenX: Float, screenY: Float, queue: Boolean) {
        if (commandMode) {
            //right click: move to position

            //move to location - TODO right click instead?

            val target = Core.input.mouseWorld(screenX, screenY).cpy()

            if (selectedUnits.size > 0) {
                var attack: Teamc? = world.buildWorld(target.x, target.y)

                if (attack == null || attack.team() === player.team()) {
                    attack = selectedEnemyUnit(target.x, target.y)
                }

                val ids = IntArray(selectedUnits.size)
                for (i in ids.indices) {
                    ids[i] = selectedUnits.get(i).id
                }

                if (attack != null) {
                    Events.fire<EventType.Trigger?>(EventType.Trigger.unitCommandAttack)
                } else {
                    Events.fire<EventType.Trigger?>(EventType.Trigger.unitCommandPosition)
                }

                val maxChunkSize = 200

                if (ids.size > maxChunkSize) {
                    var i = 0
                    while (i < ids.size) {
                        val data = Arrays.copyOfRange(ids, i, min(i + maxChunkSize, ids.size))
                        Call.commandUnits(
                            player,
                            data,
                            if (attack is Building) attack else null,
                            if (attack is Unit) attack else null,
                            target,
                            queue,
                            i + maxChunkSize >= ids.size
                        )
                        i += maxChunkSize
                    }
                } else {
                    Call.commandUnits(
                        player,
                        ids,
                        if (attack is Building) attack else null,
                        if (attack is Unit) attack else null,
                        target,
                        queue,
                        true
                    )
                }
            }

            if (commandBuildings.size > 0) {
                Call.commandBuilding(
                    player,
                    commandBuildings.mapInt(Intf { b: Building? -> b!!.pos() }).toArray(),
                    target
                )
            }
        }
    }

    fun drawCommand(sel: Unit) {
        Drawf.poly(
            sel.x,
            sel.y,
            6,
            sel.hitSize / InputHandler.unitSelectRadScl + Mathf.absin(4f, 1f),
            0f,
            if (selectedUnits.contains(sel)) Pal.remove else Pal.accent
        )
    }

    fun drawCommand(build: Building) {
        Drawf.poly(
            build.x,
            build.y,
            4,
            build.hitSize() / 1.4f + +0.5f + Mathf.absin(4f, 1f),
            0f,
            if (commandBuildings.contains(build)) Pal.remove else Pal.accent
        )
    }

    fun drawCommanded() {
        Draw.draw(Layer.plans, Runnable {
            drawCommanded(true)
        })

        Draw.draw(Layer.groundUnit - 1, Runnable {
            drawCommanded(false)
        })
    }

    fun drawCommanded(flying: Boolean) {
        val lineLimit = 6.5f
        val sides = 6
        val alpha = 0.5f

        if (commandMode) {
            //happens sometimes
            selectedUnits.removeAll(Boolf { u: Unit? -> !u!!.allowCommand() })

            //draw command overlay UI
            for (unit in selectedUnits) {
                val color = if (unit.controller() is LogicAI) Team.malis.color else Pal.accent

                var lastPos: Position? = null

                if (unit.controller() is CommandAI) {
                    val cmd: UnitCommand = ai.currentCommand()
                    lastPos = if (ai.attackTarget != null) ai.attackTarget else ai.targetPos

                    if (flying && ai.attackTarget != null && cmd.drawTarget) {
                        Drawf.target(ai.attackTarget.getX(), ai.attackTarget.getY(), 6f, Pal.remove)
                    }

                    if (unit.isFlying() != flying) continue

                    //draw target line
                    if (ai.targetPos != null && cmd.drawTarget) {
                        val lineDest: Position = if (ai.attackTarget != null) ai.attackTarget else ai.targetPos
                        Drawf.limitLine(
                            unit,
                            lineDest,
                            unit.hitSize / InputHandler.unitSelectRadScl + 1f,
                            lineLimit,
                            color.write(Tmp.c1).a(alpha)
                        )

                        if (ai.attackTarget == null) {
                            Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f, color.write(Tmp.c1).a(alpha))

                            if (cmd === UnitCommand.enterPayloadCommand) {
                                val build = world.buildWorld(lineDest.getX(), lineDest.getY())
                                if (build != null && build.block.acceptsUnitPayloads && build.team === unit.team) {
                                    Drawf.selected(build, color)
                                } else {
                                    Drawf.cross(lineDest.getX(), lineDest.getY(), 7f, Pal.remove)
                                }
                            }
                        }
                    }
                }

                val rad = unit.hitSize / InputHandler.unitSelectRadScl + 1f

                Fill.lightInner(
                    unit.x, unit.y, sides,
                    max(0f, rad * 0.8f),
                    rad,
                    0f,
                    Tmp.c3.set(color).a(0f),
                    Tmp.c2.set(color).a(0.7f)
                )

                Lines.stroke(1f)
                Draw.color(color)
                Lines.poly(unit.x, unit.y, sides, rad + 0.5f)
                //uncomment for a dark border
                //Draw.color(Pal.gray);
                //Lines.poly(unit.x, unit.y, sides, rad + 1.5f);
                Draw.reset()

                if (lastPos == null) {
                    lastPos = unit
                }

                if (unit.controller() is CommandAI) {
                    //draw command queue
                    if (ai.currentCommand().drawTarget && ai.commandQueue.size > 0) {
                        for (next in ai.commandQueue) {
                            Drawf.limitLine(lastPos, next, lineLimit, lineLimit, color.write(Tmp.c1).a(alpha))
                            lastPos = next

                            if (next is Vec2) {
                                Drawf.square(next.x, next.y, 3.5f, color.write(Tmp.c1).a(alpha))
                            } else {
                                Drawf.target(next.getX(), next.getY(), 6f, Pal.remove)
                            }
                        }
                    }

                    if (ai.targetPos != null && ai.currentCommand() === UnitCommand.loopPayloadCommand && unit is Payloadc) {
                        Draw.color(color, 0.4f + Mathf.absin(5f, 0.5f))
                        var region = if (unit.hasPayload()) Icon.download.getRegion() else Icon.upload.getRegion()
                        val offset = 11f
                        val size = 8f
                        Draw.rect(region, ai.targetPos.x, ai.targetPos.y + offset, size, size / region.ratio())

                        if (ai.commandQueue.size > 0) {
                            region = if (!unit.hasPayload()) Icon.download.getRegion() else Icon.upload.getRegion()
                            Draw.rect(
                                region,
                                ai.commandQueue.first().getX(),
                                ai.commandQueue.first().getY() + offset,
                                size,
                                size / region.ratio()
                            )
                        }
                        Draw.color()
                    }
                }
            }

            if (flying) {
                val color = Pal.accent
                for (commandBuild in commandBuildings) {
                    if (commandBuild != null) {
                        Drawf.square(commandBuild.x, commandBuild.y, commandBuild.hitSize() / 1.4f + 1f)
                        val cpos = commandBuild.getCommandPosition()

                        if (cpos != null) {
                            Drawf.limitLine(
                                commandBuild,
                                cpos,
                                commandBuild.hitSize() / 2f,
                                lineLimit,
                                color.write(Tmp.c1).a(alpha)
                            )
                            Drawf.square(cpos.x, cpos.y, 3.5f, color.write(Tmp.c1).a(alpha))
                        }
                    }
                }
            }
        }

        Draw.reset()
    }

    fun drawUnitSelection() {
        if (commandRect && commandMode) {
            val x2 = Core.input.mouseWorldX()
            val y2 = Core.input.mouseWorldY()
            val units = selectedCommandUnits(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY)
            for (unit in units) {
                drawCommand(unit)
            }
            if (units.isEmpty()) {
                val buildings =
                    selectedCommandBuildings(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY)
                for (build in buildings) {
                    drawCommand(build)
                }
            }

            Draw.color(Pal.accent, 0.3f)
            Fill.crect(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY)
        }

        if (commandMode && !commandRect) {
            val sel = selectedCommandUnit(Core.input.mouseWorldX(), Core.input.mouseWorldY())

            if (sel != null && !(!multiUnitSelect() && selectedUnits.size == 1 && selectedUnits.contains(sel))) {
                drawCommand(sel)
            }
        }
    }

    open fun drawBottom() {
    }

    open fun drawTop() {
    }

    open fun drawOverSelect() {
    }

    fun drawOverlapCheck(block: Block, cursorX: Int, cursorY: Int, valid: Boolean) {
        if (!valid && state.rules.placeRangeCheck) {
            val blocker = Build.getEnemyOverlap(block, player.team(), cursorX, cursorY)
            if (blocker != null && blocker.wasVisible) {
                Drawf.selected(blocker, Pal.remove)
                Tmp.v1.set(cursorX.toFloat(), cursorY.toFloat()).scl(tilesize.toFloat()).add(block.offset, block.offset)
                    .sub(blocker).scl(-1f).nor()
                Drawf.dashLineDst(
                    Pal.remove,
                    cursorX * tilesize + block.offset + Tmp.v1.x * block.size * tilesize / 2f,
                    cursorY * tilesize + block.offset + Tmp.v1.y * block.size * tilesize / 2f,
                    blocker.x + Tmp.v1.x * -blocker.block.size * tilesize / 2f,
                    blocker.y + Tmp.v1.y * -blocker.block.size * tilesize / 2f
                )
            }
        }
    }

    fun useSchematic(schem: Schematic?) {
        useSchematic(schem, true)
    }

    abstract fun useSchematic(schem: Schematic?, checkHidden: Boolean)

    protected fun showSchematicSave() {
        if (lastSchematic == null) return

        val last = lastSchematic

        ui.showTextInput("@schematic.add", "@name", 1000, "", Cons { text: String? ->
            val replacement = schematics.all().find(Boolf { s: Schematic? -> s!!.name() == text })
            if (replacement != null) {
                ui.showConfirm("@confirm", "@schematic.replace", Runnable {
                    schematics.overwrite(replacement, last)
                    ui.showInfoFade("@schematic.saved")
                    ui.schematics.showInfo(replacement)
                })
            } else {
                last!!.tags.put("name", text)
                last.tags.put("description", "")
                schematics.add(last)
                ui.showInfoFade("@schematic.saved")
                ui.schematics.showInfo(last)
                Events.fire<SchematicCreateEvent?>(SchematicCreateEvent(last))
            }
        })
    }

    fun rotatePlans(plans: Seq<BuildPlan?>, direction: Int) {
        val ox = schemOriginX()
        val oy = schemOriginY()

        plans.each(Cons { plan: BuildPlan? ->
            if (plan!!.breaking) return@each
            val off = if (plan.block.size % 2 == 0) -0.5f else 0f

            plan.pointConfig(Cons { p: Point2? ->
                var cx = p!!.x + off
                var cy = p.y + off
                val lx = cx

                if (direction >= 0) {
                    cx = -cy
                    cy = lx
                } else {
                    cx = cy
                    cy = -lx
                }
                p.set(Mathf.floor(cx - off), Mathf.floor(cy - off))
            })

            //rotate actual plan, centered on its multiblock position
            var wx = (plan.x - ox) * tilesize + plan.block.offset
            var wy = (plan.y - oy) * tilesize + plan.block.offset
            val x = wx
            if (direction >= 0) {
                wx = -wy
                wy = x
            } else {
                wx = wy
                wy = -x
            }
            plan.x = World.toTile(wx - plan.block.offset) + ox
            plan.y = World.toTile(wy - plan.block.offset) + oy
            plan.rotation = plan.block.planRotation(Mathf.mod(plan.rotation + direction, 4))
        })
    }

    fun flipPlans(plans: Seq<BuildPlan?>, x: Boolean) {
        val origin = (if (x) schemOriginX() else schemOriginY()) * tilesize

        plans.each(Cons { plan: BuildPlan? ->
            if (plan!!.breaking) return@each
            val value = -((if (x) plan.x else plan.y) * tilesize - origin + plan.block.offset) + origin

            if (x) {
                plan.x = ((value - plan.block.offset) / tilesize).toInt()
            } else {
                plan.y = ((value - plan.block.offset) / tilesize).toInt()
            }

            plan.pointConfig(Cons { p: Point2? ->
                if (x) {
                    if (plan.block.size % 2 == 0) p!!.x--
                    p!!.x = -p.x
                } else {
                    if (plan.block.size % 2 == 0) p!!.y--
                    p!!.y = -p.y
                }
            })

            //flip rotation
            plan.block.flipRotation(plan, x)
        })
    }

    protected open fun schemOriginX(): Int {
        return rawTileX()
    }

    protected open fun schemOriginY(): Int {
        return rawTileY()
    }

    /** @return the selection plan that overlaps this position, or null.
     */
    @Nullable
    protected fun getPlan(x: Int, y: Int): BuildPlan? {
        return getPlan(x, y, 1, null)
    }

    /** Returns the selection plan that overlaps this position, or null.  */
    @Nullable
    protected fun getPlan(x: Int, y: Int, size: Int, skip: BuildPlan?): BuildPlan? {
        val offset = ((size + 1) % 2) * tilesize / 2f
        InputHandler.r2.setSize((tilesize * size).toFloat())
        InputHandler.r2.setCenter(x * tilesize + offset, y * tilesize + offset)
        resultplan = null

        val test = Boolf { plan: BuildPlan? ->
            if (plan === skip) return@Boolf false
            val other = plan!!.tile()

            if (other == null) return@Boolf false

            if (!plan.breaking) {
                InputHandler.r1.setSize((plan.block.size * tilesize).toFloat())
                InputHandler.r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset)
            } else {
                InputHandler.r1.setSize((other.block().size * tilesize).toFloat())
                InputHandler.r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset)
            }
            InputHandler.r2.overlaps(InputHandler.r1)
        }

        if (!player.dead()) {
            for (plan in player.unit().plans()) {
                if (test.get(plan)) return plan
            }
        }

        return selectPlans.find(test)
    }

    protected fun drawBreakSelection(x1: Int, y1: Int, x2: Int, y2: Int, maxLength: Int) {
        drawBreakSelection(x1, y1, x2, y2, maxLength, true)
    }

    protected fun drawBreakSelection(x1: Int, y1: Int, x2: Int, y2: Int, maxLength: Int, useSelectPlans: Boolean) {
        val result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f)
        val dresult = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength)

        for (x in dresult.x..dresult.x2) {
            for (y in dresult.y..dresult.y2) {
                val tile = world.tileBuilding(x, y)
                if (tile == null || !validBreak(tile.x.toInt(), tile.y.toInt())) continue

                drawBreaking(tile.x.toInt(), tile.y.toInt())
            }
        }

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y)

        Draw.color(Pal.remove)
        Lines.stroke(1f)

        if (!player.dead()) {
            for (plan in player.unit().plans()) {
                if (!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)) {
                    drawBreaking(plan)
                }
            }
        }

        if (useSelectPlans) {
            for (plan in selectPlans) {
                if (!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)) {
                    drawBreaking(plan)
                }
            }
        }

        for (plan in player.team().data().plans) {
            val block = plan.block
            if (block.bounds(plan.x.toInt(), plan.y.toInt(), Tmp.r2).overlaps(Tmp.r1)) {
                drawSelected(plan.x.toInt(), plan.y.toInt(), plan.block, Pal.remove)
            }
        }

        Lines.stroke(2f)

        Draw.color(Pal.removeBack)
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y)
        Draw.color(Pal.remove)
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y)
    }

    protected fun drawRebuildSelection(x1: Int, y1: Int, x2: Int, y2: Int) {
        drawSelection(x1, y1, x2, y2, 0, Pal.sapBulletBack, Pal.sapBullet, false)

        val result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, 0, 1f)

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y)

        for (plan in player.team().data().plans) {
            val block = plan.block
            if (block.bounds(plan.x.toInt(), plan.y.toInt(), Tmp.r2).overlaps(Tmp.r1)) {
                drawSelected(plan.x.toInt(), plan.y.toInt(), plan.block, Pal.sapBullet)
            }
        }

        val dresult = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, 999999999)

        InputHandler.intSet.clear()
        for (x in dresult.x..dresult.x2) {
            for (y in dresult.y..dresult.y2) {
                val tile = world.tileBuilding(x, y)

                if (tile != null && InputHandler.intSet.add(tile.pos()) && canRepairDerelict(tile)) {
                    drawSelected(tile.x.toInt(), tile.y.toInt(), tile.block(), Pal.sapBullet)
                }
            }
        }
    }

    protected fun drawBreakSelection(x1: Int, y1: Int, x2: Int, y2: Int) {
        drawBreakSelection(x1, y1, x2, y2, InputHandler.maxLength)
    }

    protected fun drawSelection(x1: Int, y1: Int, x2: Int, y2: Int, maxLength: Int) {
        drawSelection(x1, y1, x2, y2, maxLength, Pal.accentBack, Pal.accent, true)
    }

    protected fun drawSelection(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        maxLength: Int,
        col1: Color,
        col2: Color,
        withText: Boolean
    ) {
        val result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f)

        Lines.stroke(2f)

        Draw.color(col1)
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y)
        Draw.color(col2)
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y)

        if (withText) {
            val font = Fonts.outline
            font.setColor(col2)
            val ints = font.usesIntegerPositions()
            font.setUseIntegerPositions(false)
            val z = Draw.z()
            Draw.z(Layer.endPixeled)
            font.getData().setScale(1 / renderer.camerascale)
            val snapToCursor = Core.settings.getBool("selectionsizeoncursor")
            val textOffset = Core.settings.getInt("selectionsizeoncursoroffset", 5)
            val width = ((result.x2 - result.x) / 8).toInt()
            val height = ((result.y2 - result.y) / 8).toInt()
            val area = width * height

            // FINISHME: When not snapping to cursor, perhaps it would be best to choose the corner closest to the cursor that's at least a block away?
            font.draw(
                width.toString() + "x" + height + " (" + area + ")",
                if (snapToCursor) Core.input.mouseWorldX() + textOffset * (4 / renderer.camerascale) else result.x2,
                if (snapToCursor) Core.input.mouseWorldY() - textOffset * (4 / renderer.camerascale) else result.y
            )
            font.setColor(Color.white)
            font.getData().setScale(1f)
            font.setUseIntegerPositions(ints)
            Draw.z(z)
        }
    }

    /** Remove everything from the queue in a selection.  */
    protected fun removeSelection(x1: Int, y1: Int, x2: Int, y2: Int) {
        removeSelection(x1, y1, x2, y2, false)
    }

    /** Remove everything from the queue in a selection.  */
    protected fun removeSelection(x1: Int, y1: Int, x2: Int, y2: Int, maxLength: Int) {
        removeSelection(x1, y1, x2, y2, false, maxLength)
    }

    /** Remove everything from the queue in a selection.  */
    protected fun removeSelection(x1: Int, y1: Int, x2: Int, y2: Int, flush: Boolean) {
        removeSelection(x1, y1, x2, y2, flush, InputHandler.maxLength)
    }


    /** Handles tile tap events that are not platform specific.  */
    fun tileTapped(@Nullable build: Building?): Boolean {
        planConfig.hide()
        if (build == null) {
            inv.hide()
            config.hideConfig()
            commandBuildings.clear()
            return false
        }
        var consumed = false
        var showedInventory = false

        //select building for commanding
        if (build.isCommandable() && commandMode) {
            //TODO handled in tap.
            consumed = true
        } else if (build.block.configurable && build.interactable(player.team())) { //check if tapped block is configurable
            consumed = true
            if ((!config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
                //alternatively, the current selected block can 'agree' to switch config tiles
                || (config.isShown() && config.getSelected().onConfigureBuildTapped(build) && build.shouldShowConfigure(
                    player
                ))
            ) {
                Sounds.click.at(build)
                config.showConfig(build)
            }
            //otherwise...
        } else if (!config.hasConfigMouse()) { //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if (config.isShown() && config.getSelected().onConfigureBuildTapped(build)) {
                consumed = true
                config.hideConfig()
            }

            if (config.isShown()) {
                consumed = true
            }
        }

        //call tapped event
        if (!consumed && build.interactable(player.team())) {
            build.tapped()
        }

        //consume tap event if necessary
        if (build.interactable(player.team()) && build.block.consumesTap) {
            consumed = true
        } else if (build.interactable(player.team()) && build.block.synthetic() && (!consumed || build.block.allowConfigInventory)) {
            if (build.block.hasItems && build.items.total() > 0) {
                inv.showFor(build)
                consumed = true
                showedInventory = true
            }
        }

        if (!showedInventory) {
            inv.hide()
        }

        return consumed
    }

    /** Tries to select the player to drop off items, returns true if successful.  */
    fun tryTapPlayer(x: Float, y: Float): Boolean {
        if (canTapPlayer(x, y)) {
            droppingItem = true
            return true
        }
        return false
    }

    fun canTapPlayer(x: Float, y: Float): Boolean {
        return player.within(
            x,
            y,
            InputHandler.playerSelectRange
        ) && !player.dead() && player.unit().stack.amount > 0 && block == null
    }

    /** Tries to begin mining a tile, returns true if successful.  */
    fun tryBeginMine(tile: Tile): Boolean {
        if (!player.dead() && canMine(tile)) {
            player.unit().mineTile = tile
            return true
        }
        return false
    }

    /** Tries to stop mining, returns true if mining was stopped.  */
    fun tryStopMine(): Boolean {
        if (!player.dead() && player.unit().mining()) {
            player.unit().mineTile = null
            return true
        }
        return false
    }

    fun tryStopMine(tile: Tile?): Boolean {
        if (!player.dead() && player.unit().mineTile === tile) {
            player.unit().mineTile = null
            return true
        }
        return false
    }



    fun canRepairDerelict(tile: Tile?): Boolean {
        return tile != null && tile.build != null && !player.dead() && !state.rules.editor && player.team() !== Team.derelict && tile.build.team === Team.derelict && tile.build.block.unlockedNowHost() &&
                Build.validPlace(
                    tile.block(),
                    player.team(),
                    tile.build.tileX(),
                    tile.build.tileY(),
                    tile.build.rotation
                )
    }

    fun canMine(tile: Tile): Boolean {
        return !Core.scene.hasMouse() && !player.dead() && player.unit().validMine(tile)
                && player.unit().acceptsItem(player.unit().getMineResult(tile))
                && !((!Core.settings.getBool("doubletapmine") && tile.floor().playerUnmineable) && tile.overlay().itemDrop == null)
    }

    /** Returns the tile at the specified MOUSE coordinates.  */
    fun tileAt(x: Float, y: Float): Tile? {
        return world.tile(tileX(x), tileY(y))
    }

    fun rawTileX(): Int {
        return World.toTile(Core.input.mouseWorld().x)
    }

    fun rawTileY(): Int {
        return World.toTile(Core.input.mouseWorld().y)
    }

    fun tileX(cursorX: Float): Int {
        val vec = Core.input.mouseWorld(cursorX, 0f)
        if (selectedBlock()) {
            vec.sub(block.offset, block.offset)
        }
        return World.toTile(vec.x)
    }

    fun tileY(cursorY: Float): Int {
        val vec = Core.input.mouseWorld(0f, cursorY)
        if (selectedBlock()) {
            vec.sub(block.offset, block.offset)
        }
        return World.toTile(vec.y)
    }

    /** Forces the camera to a position and enables panning on desktop.  */
    open fun panCamera(position: Vec2?) {
        if (!locked()) {
            Core.camera.position.set(position)
        }
    }

    open fun selectedBlock(): Boolean {
        return isPlacing()
    }

    open fun isPlacing(): Boolean {
        return block != null
    }

    open fun isBreaking(): Boolean {
        return false
    }

    open fun isRebuildSelecting(): Boolean {
        return Core.input.keyDown(Binding.rebuildSelect)
    }

    fun mouseAngle(x: Float, y: Float): Float {
        return Core.input.mouseWorld(getMouseX(), getMouseY()).sub(x, y).angle()
    }

    @Nullable
    fun selectedCommandUnit(x: Float, y: Float): Unit? {
        val tree = player.team().data().tree()
        InputHandler.tmpUnits.clear()
        val rad = 4f
        tree.intersect(x - rad / 2f, y - rad / 2f, rad, rad, InputHandler.tmpUnits)
        return InputHandler.tmpUnits.min(
            Boolf { u: Unit? -> u!!.isCommandable() },
            Floatf { u: Unit? -> u!!.dst(x, y) - u.hitSize / 2f })
    }

    @Nullable
    fun selectedEnemyUnit(x: Float, y: Float): Unit? {
        InputHandler.tmpUnits.clear()
        val rad = 4f

        val data = state.teams.present
        for (i in 0..<data.size) {
            if (data.items[i]!!.team !== player.team()) {
                data.items[i]!!.tree().intersect(x - rad / 2f, y - rad / 2f, rad, rad, InputHandler.tmpUnits)
            }
        }

        return InputHandler.tmpUnits.min(
            Boolf { u: Unit? -> !u!!.inFogTo(player.team()) },
            Floatf { u: Unit? -> u!!.dst(x, y) - u.hitSize / 2f })
    }

    fun selectedCommandBuildings(x: Float, y: Float, w: Float, h: Float): Seq<Building> {
        val tree = player.team().data().buildingTree
        InputHandler.tmpBuildings.clear()
        if (tree == null) return InputHandler.tmpBuildings
        val rad = 4f
        tree.intersect(
            Tmp.r1.set(x - rad / 2f, y - rad / 2f, rad * 2f + w, rad * 2f + h).normalize(),
            Cons { b: Building? ->
                if (b!!.isCommandable()) {
                    InputHandler.tmpBuildings.add(b)
                }
            })
        return InputHandler.tmpBuildings
    }

    fun selectedCommandUnits(x: Float, y: Float, w: Float, h: Float, predicate: Boolf<Unit?>): Seq<Unit> {
        val tree = player.team().data().tree()
        InputHandler.tmpUnits.clear()
        val rad = 4f
        tree.intersect(
            Tmp.r1.set(x - rad / 2f, y - rad / 2f, rad * 2f + w, rad * 2f + h).normalize(),
            InputHandler.tmpUnits
        )
        InputHandler.tmpUnits.removeAll(Boolf { u: Unit? -> !u!!.isCommandable() || !predicate.get(u) })
        return InputHandler.tmpUnits
    }

    fun selectedCommandUnits(x: Float, y: Float, w: Float, h: Float): Seq<Unit> {
        return selectedCommandUnits(x, y, w, h, Boolf { u: Unit? -> true })
    }

    fun remove() {
        Core.input.removeProcessor(this)
        group.remove()
        if (Core.scene != null) {
            val table = Core.scene.find("inputTable") as Table?
            if (table != null) {
                table.clear()
            }
        }
        if (detector != null) {
            Core.input.removeProcessor(detector)
        }
        if (uiGroup != null) {
            uiGroup.remove()
            uiGroup = null
        }
    }

    fun add() {
        Core.input.getInputProcessors()
            .remove(Boolf { i: InputProcessor? -> i is InputHandler || (i is GestureDetector && i.getListener() is InputHandler) })
        Core.input.addProcessor(GestureDetector(20f, 0.5f, 0.3f, 0.15f, this).also { detector = it })
        Core.input.addProcessor(this)
        if (Core.scene != null) {
            val table = Core.scene.find("inputTable") as Table?
            if (table != null) {
                table.clear()
                buildPlacementUI(table)
            }

            uiGroup = WidgetGroup()
            uiGroup.touchable = Touchable.childrenOnly
            uiGroup.setFillParent(true)
            ui.hudGroup.addChild(uiGroup)
            uiGroup.toBack()
            buildUI(uiGroup)

            group.setFillParent(true)
            ui.hudGroup.addChildBefore(Core.scene.find("overlaymarker"), group)

            inv.build(group)
            config.build(group)
            planConfig.build(group)
        }
    }

    fun canShoot(): Boolean {
        return block == null && !onConfigurable() && !isDroppingItem() && !player.unit()
            .activelyBuilding() && !(player.unit() is Mechc && player.unit().isFlying()) && !player.unit()
            .mining() && !commandMode
    }

    fun onConfigurable(): Boolean {
        return false
    }

    fun isDroppingItem(): Boolean {
        return droppingItem
    }

    fun canDropItem(): Boolean {
        return droppingItem && !canTapPlayer(Core.input.mouseWorldX(), Core.input.mouseWorldY())
    }

    fun tryDropItems(@Nullable build: Building?, x: Float, y: Float) {
        if (player.dead()) return

        if (!droppingItem || player.unit().stack.amount <= 0 || canTapPlayer(x, y) || state.isPaused()) {
            droppingItem = false
            return
        }

        droppingItem = false

        val stack = player.unit().stack

        if (build != null && build.acceptStack(
                stack.item,
                stack.amount,
                player.unit()
            ) > 0 && build.interactable(player.team()) &&
            build.block.hasItems && player.unit().stack().amount > 0 && build.interactable(player.team())
        ) {
            if (build.allowDeposit() && itemDepositCooldown <= 0f) {
                Call.transferInventory(player, build)
                itemDepositCooldown = state.rules.itemDepositCooldown
            }
        } else {
            Call.dropItem(player.angleTo(x, y))
        }
    }

    fun validPlace(x: Int, y: Int, type: Block, rotation: Int): Boolean {
        return validPlace(x, y, type, rotation, null)
    }

    fun validPlace(x: Int, y: Int, type: Block, rotation: Int, @Nullable ignore: BuildPlan?): Boolean {
        return validPlace(x, y, type, rotation, ignore, false)
    }

    fun validPlace(
        x: Int,
        y: Int,
        type: Block,
        rotation: Int,
        @Nullable ignore: BuildPlan?,
        ignoreUnits: Boolean
    ): Boolean {
        if (player.isBuilder() && player.unit().plans.size > 0) {
            Tmp.r1.setCentered(x * tilesize + type.offset, y * tilesize + type.offset, (type.size * tilesize).toFloat())
            plansOut.clear()
            playerPlanTree.intersect(Tmp.r1, plansOut)

            for (i in 0..<plansOut.size) {
                val plan = plansOut.items[i]
                if (plan !== ignore && !plan.breaking && plan.block.bounds(plan.x, plan.y, Tmp.r1)
                        .overlaps(type.bounds(x, y, Tmp.r2))
                    && !(type.canReplace(plan.block) && Tmp.r1 == Tmp.r2)
                ) {
                    return false
                }
            }
        }

        return if (ignoreUnits) Build.validPlaceIgnoreUnits(
            type,
            player.team(),
            x,
            y,
            rotation,
            true,
            true
        ) else Build.validPlace(type, player.team(), x, y, rotation)
    }

    fun validBreak(x: Int, y: Int): Boolean {
        return Build.validBreak(player.team(), x, y)
    }

    fun drawArrow(block: Block, x: Int, y: Int, rotation: Int) {
        drawArrow(block, x, y, rotation, validPlace(x, y, block, rotation))
    }

    fun drawArrow(block: Block, x: Int, y: Int, rotation: Int, valid: Boolean) {
        val trns = ((block.size / 2) * tilesize).toFloat()
        val dx = Geometry.d4(rotation).x
        val dy = Geometry.d4(rotation).y
        val offsetx = x * tilesize + block.offset + dx * trns
        val offsety = y * tilesize + block.offset + dy * trns

        Draw.color(if (!valid) Pal.removeBack else Pal.accentBack)
        val regionArrow: TextureRegion = Core.atlas.find("place-arrow")

        Draw.rect(
            regionArrow,
            offsetx,
            offsety - 1,
            regionArrow.width * regionArrow.scl(),
            regionArrow.height * regionArrow.scl(),
            (rotation * 90 - 90).toFloat()
        )

        Draw.color(if (!valid) Pal.remove else Pal.accent)
        Draw.rect(
            regionArrow,
            offsetx,
            offsety,
            regionArrow.width * regionArrow.scl(),
            regionArrow.height * regionArrow.scl(),
            (rotation * 90 - 90).toFloat()
        )
    }

    */
}
