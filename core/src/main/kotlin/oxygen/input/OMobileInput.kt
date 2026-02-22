package oxygen.input

import arc.*
import arc.graphics.g2d.*
import arc.input.*
import arc.input.GestureDetector.GestureListener
import arc.math.*
import arc.math.geom.*
import arc.scene.*
import arc.scene.ui.layout.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.content.*
import mindustry.core.*
import mindustry.entities.*
import mindustry.entities.units.*
import mindustry.game.*
import mindustry.game.EventType.LineConfirmEvent
import mindustry.game.EventType.UnitDestroyEvent
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.graphics.*
import mindustry.input.*
import mindustry.ui.*
import mindustry.world.*
import mindustry.world.blocks.*
import oxygen.*
import oxygen.math.geom.*
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.also
import kotlin.let
import kotlin.math.*
import kotlin.takeIf
import kotlin.text.toInt

class OMobileInput : OInputHandler(), GestureListener {
    /** Distance to edge of screen to start panning.  */
    val edgePan: Float = Scl.scl(60f)

    //gesture data
    var vector: Vec2 = Vec2()
    var movement: Vec2 = Vec2()
    var targetPos: Vec2 = Vec2()
    var lastZoom: Float = -1f

    /** Position where the player started dragging a line.  */
    var lineStartX: Int = 0
    var lineStartY: Int = 0
    var lastLineX: Int = 0
    var lastLineY: Int = 0

    /** Animation scale for line.  */
    var lineScale: Float = 0f

    /** Animation data for crosshair.  */
    var crosshairScale: Float = 0f
    var lastTarget: Teamc? = null

    /** Used for shifting build plans.  */
    var shiftDeltaX: Float = 0f
    var shiftDeltaY: Float = 0f

    /** Place plans to be removed.  */
    var removals: Seq<BuildPlan> = Seq<BuildPlan>()

    /** Whether the player is currently shifting all placed tiles.  */
    var selecting: Boolean = false

    /** Various modes that aren't enums for some reason. This should be cleaned up.  */
    var lineMode: Boolean = false
    var schematicMode: Boolean = false
    var rebuildMode: Boolean = false
    var queueCommandMode: Boolean = false

    /** Current place mode.  */
    var mode: PlaceMode = PlaceMode.none

    /** Whether no recipe was available when switching to break mode.  */
    @Nullable
    var lastBlock: Block? = null

    /** Last placed plan. Used for drawing block overlay.  */
    @Nullable
    var lastPlaced: BuildPlan? = null

    /** Down tracking for panning.  */
    var down: Boolean = false

    /** Whether manual shooting (point with finger) is enabled.  */
    var manualShooting: Boolean = false

    /** Current thing being shot at.  */
    @Nullable
    var target: Teamc? = null

    /** Payload target being moved to. Can be a position (for dropping), or a unit/block.  */
    @Nullable
    var payloadTarget: Position? = null

    /** Unit last tapped, or null if last tap was not on a unit.  */
    @Nullable
    var unitTapped: Unit? = null

    /** Control building last tapped.  */
    @Nullable
    var buildingTapped: Building? = null

    init {
        Events.on(UnitDestroyEvent::class.java) { e ->
            if (e.unit != null && e.unit.isPlayer() && e.unit.getPlayer()
                    .isLocal() && e.unit.type.weapons.contains { w -> w.bullet.killShooter }
            ) {
                manualShooting = false
            }
        }
    }

    //region utility methods
    /** Check and assign targets for a specific position.  */
    fun checkTargets(x: Float, y: Float, cursor: Tile) {
        if (Vars.player.dead()) return

        val unit = Units.closestEnemy(Vars.player.team(), x, y, 20f) { u -> !u.dead }

        if (unit != null && Vars.player.unit().type.canAttack) {
            Vars.player.unit().mineTile = null
            target = unit
        } else {
            val tile = cursor.build

            if ((tile != null && (Vars.player.team() !== tile.team && (tile.team !== Team.derelict || Vars.state.rules.coreCapture)) && Vars.player.unit().type.canAttack) || (tile != null && Vars.player.unit().type.canHeal && tile.team === Vars.player.team() && tile.damaged())) {
                Vars.player.unit().mineTile = null
                target = tile
            }
        }
    }

    /** Returns whether this tile is in the list of plans, or at least colliding with one.  */
    fun hasPlan(tile: Tile): Boolean {
        return getPlan(tile) != null
    }

    /** Returns whether this block overlaps any selection plans.  */
    fun checkOverlapPlacement(x: Int, y: Int, tiles: Tiles, block: Block): Boolean {
        InputHandler.r2.setSize((block.size * Vars.tilesize).toFloat())
        InputHandler.r2.setCenter(x * Vars.tilesize + block.offset, y * Vars.tilesize + block.offset)

        for (plan in selectPlans) {
            if (plan.tiles !== tiles) continue
            val other = plan.tile()

            if (other == null || plan.breaking) continue

            InputHandler.r1.setSize((plan.block.size * Vars.tilesize).toFloat())
            InputHandler.r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset)

            if (InputHandler.r2.overlaps(InputHandler.r1)) {
                return true
            }
        }

        if (!Vars.player.dead()) {
            for (plan in Vars.player.unit().plans()) {
                if (plan.tiles !== tiles) continue
                val other = Vars.world.tile(plan.x, plan.y)

                if (other == null || plan.breaking) continue

                InputHandler.r1.setSize((plan.block.size * Vars.tilesize).toFloat())
                InputHandler.r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset)

                if (InputHandler.r2.overlaps(InputHandler.r1)) {
                    return true
                }
            }
        }

        return false
    }

    /** Returns the selection plan that overlaps this tile, or null.  */
    fun getPlan(tile: Tile): BuildPlan? {
        InputHandler.r2.setSize(Vars.tilesize.toFloat())
        InputHandler.r2.setCenter(tile.worldx(), tile.worldy())

        for (plan in selectPlans) {
            if (plan.tiles !== tile.tiles) continue
            val other = plan.tile()

            if (other == null) continue

            if (!plan.breaking) {
                InputHandler.r1.setSize((plan.block.size * Vars.tilesize).toFloat())
                InputHandler.r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset)
            } else {
                InputHandler.r1.setSize((other.block().size * Vars.tilesize).toFloat())
                InputHandler.r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset)
            }

            if (InputHandler.r2.overlaps(InputHandler.r1)) return plan
        }
        return null
    }

    fun removePlan(plan: BuildPlan) {
        selectPlans.remove(plan, true)
        if (!plan.breaking) {
            removals.add(plan)
        }
    }

    val isLinePlacing: Boolean
        get() = mode == PlaceMode.placing && lineMode && Mathf.dst(
            (lineStartX * Vars.tilesize).toFloat(),
            (lineStartY * Vars.tilesize).toFloat(),
            Core.input.mouseWorld().x,
            Core.input.mouseWorld().y
        ) >= 3 * Vars.tilesize

    val isAreaBreaking: Boolean
        get() = mode == PlaceMode.breaking && lineMode && Mathf.dst(
            (lineStartX * Vars.tilesize).toFloat(),
            (lineStartY * Vars.tilesize).toFloat(),
            Core.input.mouseWorld().x,
            Core.input.mouseWorld().y
        ) >= 2 * Vars.tilesize

    //endregion
    //region UI and drawing
    override fun buildPlacementUI(table: Table) {
        table.left().margin(0f).defaults().size(48f)

        table.button(Icon.hammer, Styles.clearNoneTogglei) {
            mode =
                if (mode == PlaceMode.breaking) if (block == null) PlaceMode.none else PlaceMode.placing else PlaceMode.breaking
            lastBlock = block
        }.update { l -> l.setChecked(mode == PlaceMode.breaking) }.name("breakmode")

        //diagonal swap button
        table.button(Icon.diagonal, Styles.clearNoneTogglei) {
            Core.settings.put("swapdiagonal", !Core.settings.getBool("swapdiagonal"))
        }.update { l -> l.setChecked(Core.settings.getBool("swapdiagonal")) }

        //rotate button
        table.button(Icon.right, Styles.clearNoneTogglei) {
            if (block != null && block.rotate) {
                rotation = Mathf.mod(rotation + 1, 4)
            } else {
                schematicMode = !schematicMode
                if (schematicMode) {
                    block = null
                    mode = PlaceMode.none
                } else {
                    rebuildMode = false
                }
            }
        }.update { i ->
            val arrow = block != null && block.rotate
            i.getImage().setRotationOrigin((if (!arrow) 0 else rotation * 90).toFloat(), Align.center)
            i.getStyle().imageUp = if (arrow) Icon.right else Icon.copy
            i.setChecked(!arrow && schematicMode)
        }

        //confirm button
        table.button(Icon.ok, Styles.clearNoneTogglei) {
            if (schematicMode) {
                rebuildMode = !rebuildMode
            } else if (!Vars.player.dead()) {
                for (plan in selectPlans) {
                    val tile = plan.tile()

                    //actually place/break all selected blocks
                    if (tile != null) {
                        if (!plan.breaking) {
                            if (validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation, null, true)) {
                                val other = getPlan(plan.x, plan.y, plan.tiles, plan.block.size, null)
                                val copy = plan.copy()

                                if (other == null) {
                                    Vars.player.unit().addBuild(copy)
                                } else if (!other.breaking && other.tiles == plan.tiles && other.x == plan.x && other.y == plan.y && other.block.size == plan.block.size) {
                                    Vars.player.unit().plans().remove(other)
                                    Vars.player.unit().addBuild(copy)
                                }
                            }

                            rotation = plan.rotation
                        } else {
                            tryBreakBlock(tile.x.toInt(), tile.y.toInt(), plan.tiles)
                        }
                    }
                }

                //move all current plans to removal array so they fade out
                removals.addAll(selectPlans.select { r -> !r.breaking })
                selectPlans.clear()
                selecting = false
            }
        }.visible { !selectPlans.isEmpty() || schematicMode || rebuildMode }.update { i ->
            i!!.getStyle().imageUp = if (schematicMode || rebuildMode) Icon.wrench else Icon.ok
            i.setChecked(rebuildMode)
            i.setDisabled { Vars.player.dead() }
        }.name("confirmplace")
    }

    fun showCancel(): Boolean {
        return !Vars.player.dead() && (Vars.player.unit()
            .isBuilding() || block != null || mode == PlaceMode.breaking || !selectPlans.isEmpty()) && !hasSchematic()
    }

    fun hasSchematic(): Boolean {
        return lastSchematic != null && !selectPlans.isEmpty()
    }

    override fun buildUI(group: Group) {
        group.fill { t ->
            t.visible { this.showCancel() }
            t.bottom().left()
            t.button("@cancel", Icon.cancel, Styles.clearTogglet) {
                if (!Vars.player.dead()) {
                    Vars.player.unit().clearBuilding()
                }
                selectPlans.clear()
                mode = PlaceMode.none
                block = null
            }.width(155f).checked { b -> false }.height(50f).margin(12f)
        }

        group.fill { t ->
            t.visible { !hasSchematic() && !(Vars.state.isEditor() && Core.settings.getBool("editor-blocks-shown")) }
            t.bottom().left()

            t.button("@command.queue", Icon.rightOpen, Styles.clearTogglet) {
                queueCommandMode = !queueCommandMode
            }.width(155f).height(48f).margin(12f).checked { b -> queueCommandMode }
                .visible { commandMode }.row()

            t.button("@command", Icon.units, Styles.clearTogglet) {
                commandMode = !commandMode
                if (commandMode) {
                    block = null
                    rebuildMode = false
                    mode = PlaceMode.none
                }
            }.width(155f).height(48f).margin(12f).checked { b -> commandMode }.row()

            t.spacerY { if (showCancel()) 50f else 0f }.row()

            //for better looking insets
            t.rect { x, y, w, h ->
                if (Core.scene.marginBottom > 0) {
                    Tex.paneRight.draw(x, 0f, w, y)
                }
            }.fillX().row()
        }

        group.fill { t ->
            t.visible { this.hasSchematic() }
            t.bottom().left()
            t.table(Tex.pane) { b ->
                b.defaults().size(50f)
                val style = Styles.clearNonei

                b.button(Icon.save, style) { this.showSchematicSave() }
                    .disabled { f -> lastSchematic == null || lastSchematic.file != null }
                b.button(Icon.cancel, style) {
                    selectPlans.clear()
                    lastSchematic = null
                }
                b.row()
                b.button(Icon.flipX, style) { flipPlans(selectPlans, true) }
                b.button(Icon.flipY, style) { flipPlans(selectPlans, false) }
                b.row()
                b.button(Icon.rotate, style) { rotatePlans(selectPlans, 1) }
                    .update { i ->
                        val img = i.getCells().first().get()
                        img.setScale(-1f, 1f)
                        //why the heck doesn't setOrigin work for scaling
                        img.setTranslation(img.getWidth(), 0f)
                    }
            }.margin(4f)
        }
    }

    override fun drawBottom() {
        Lines.stroke(1f)

        //draw plans about to be removed
        for (entry in genTilesPlans(removals)) {
            val tiles = entry.key
            val plans = entry.value
            drawTiles = tiles
            Oxygen.renderer.drawTilesFunc(tiles.craft) {
                for (plan in plans) {
                    val tile = plan.tile()
                    if (tile == null) continue

                    plan.animScale = Mathf.lerpDelta(plan.animScale, 0f, 0.2f)

                    if (plan.breaking) {
                        drawSelected(plan.x, plan.y, tile.block(), Pal.remove)
                    } else {
                        plan.block.drawPlan(plan, allPlans(), true)
                    }
                }
            }
        }

        Draw.mixcol()
        Draw.color(Pal.accent)

        //Draw lines
        if (lineMode) {
            val tiles = lineTiles
            val (tileX, tileY) = rawTilePos(tiles)
            drawTiles = tiles
            Oxygen.renderer.drawTilesFunc(tiles.craft) {

                if (mode == PlaceMode.placing && block != null) {
                    //draw placing
                    for (i in 0..<linePlans.size) {
                        val plan = linePlans.get(i)
                        if (i == linePlans.size - 1 && plan.block.rotate && plan.block.drawArrow) {
                            drawArrow(block, plan.x, plan.y, plan.rotation)
                        }
                        plan.block.drawPlan(
                            plan,
                            allPlans(),
                            validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation) && getPlan(
                                plan.x,
                                plan.y,
                                plan.tiles,
                                plan.block.size,
                                null
                            ) == null
                        )
                        drawSelected(plan.x, plan.y, plan.block, Pal.accent)
                    }
                    linePlans.each { plan -> this.drawOverPlan(plan) }
                } else if (mode == PlaceMode.breaking) {
                    drawBreakSelection(lineStartX, lineStartY, tileX.toInt(), tileY.toInt())
                }
            }
        }

        Draw.reset()
    }

    override fun drawTop() {
        if (mode == PlaceMode.schematicSelect) {
            val tiles = lineTiles
            drawTiles = tiles
            Oxygen.renderer.drawTilesFunc(tiles.craft) {
                drawSelection(lineStartX, lineStartY, lastLineX, lastLineY, Vars.maxSchematicSize)
            }
        } else if (mode == PlaceMode.rebuildSelect) {
            drawRebuildSelection(lineStartX, lineStartY, lastLineX, lastLineY)
        }
    }

    override fun drawOverSelect() {
        //draw list of plans
        for (entry in genTilesPlans(selectPlans)) {
            val tiles = entry.key
            val plans = entry.value
            drawTiles = tiles
            Oxygen.renderer.drawTilesFunc(tiles.craft) {
                for (plan in plans) {
                    val tile = plan.tile()

                    if (tile == null) continue

                    if ((!plan.breaking && validPlace(
                            tile.x.toInt(),
                            tile.y.toInt(),
                            plan.tiles,
                            plan.block,
                            plan.rotation
                        ))
                        || (plan.breaking && validBreak(tile.x.toInt(), tile.y.toInt(), tile.tiles))
                    ) {
                        plan.animScale = Mathf.lerpDelta(plan.animScale, 1f, 0.2f)
                    } else {
                        plan.animScale = Mathf.lerpDelta(plan.animScale, 0.6f, 0.1f)
                    }

                    Tmp.c1.set(Draw.getMixColor())

                    if (!plan.breaking && plan === lastPlaced && plan.block != null) {
                        Draw.mixcol()
                        if (plan.block.rotate && plan.block.drawArrow) drawArrow(
                            plan.block,
                            tile.x.toInt(),
                            tile.y.toInt(),
                            plan.rotation
                        )
                    }

                    Draw.reset()
                    drawPlan(plan)
                    if (!plan.breaking) {
                        drawOverPlan(plan)
                    }

                    //draw last placed plan
                    if (!plan.breaking && plan === lastPlaced && plan.block != null) {
                        val rot = plan.block.planRotation(plan.rotation)
                        val valid = validPlace(tile.x.toInt(), tile.y.toInt(), plan.tiles, plan.block, rot)
                        Draw.mixcol()
                        plan.block.drawPlace(tile.x.toInt(), tile.y.toInt(), rot, valid)

                        drawOverlapCheck(plan.block, tile.x.toInt(), tile.y.toInt(), valid)
                    }

                }
            }
        }

        //draw targeting crosshair
        if (target != null && !Vars.state.isEditor() && !manualShooting) {
            if (target !== lastTarget) {
                crosshairScale = 0f
                lastTarget = target
            }

            crosshairScale = Mathf.lerpDelta(crosshairScale, 1f, 0.2f)

            Drawf.target(target!!.getX(), target!!.getY(), 7f * Interp.swingIn.apply(crosshairScale), Pal.remove)
        }

        Draw.reset()
    }

    override fun drawPlan(plan: BuildPlan) {
        if (plan.tile() == null) return
        plan.animScale = Mathf.lerpDelta(plan.animScale, 1f, 0.1f)
        bplan.animScale = plan.animScale

        if (plan.breaking) {
            drawSelected(plan.x, plan.y, plan.tile().block(), Pal.remove)
        } else {
            plan.block.drawPlan(plan, allPlans(), validPlace(plan.x, plan.y, plan.tiles, plan.block, plan.rotation))
            drawSelected(plan.x, plan.y, plan.block, Pal.accent)
        }
    }

    //endregion
    //region input events, overrides
    override fun isRebuildSelecting(): Boolean {
        return rebuildMode
    }

    override fun schemOriginX(): Int {
        Tmp.v1.setZero()
        selectPlans.each { r -> Tmp.v1.add(r.drawx(), r.drawy()) }
        return World.toTile(Tmp.v1.scl(1f / selectPlans.size).x)
    }

    override fun schemOriginY(): Int {
        Tmp.v1.setZero()
        selectPlans.each { r -> Tmp.v1.add(r.drawx(), r.drawy()) }
        return World.toTile(Tmp.v1.scl(1f / selectPlans.size).y)
    }

    override fun isPlacing(): Boolean {
        return super.isPlacing() && mode == PlaceMode.placing
    }

    override fun isBreaking(): Boolean {
        return mode == PlaceMode.breaking
    }

    override fun useSchematic(schem: Schematic, checkHidden: Boolean) {
        val tile = getTile(Core.camera.position.x, Core.camera.position.y)
        if (tile == null) return
        val tiles = tile.tiles
        selectPlans.clear()
        selectPlans.addAll(
            Vars.schematics.toPlans(
                schem,
                tile.x.toInt(),
                tile.y.toInt(),
                tiles,
                checkHidden
            )
        )
        lastSchematic = schem
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        if (Vars.state.isMenu() || locked()) return false

        down = true

        if (Vars.player.dead()) return false

        //get tile on cursor
        val cursor = tileAt(screenX.toFloat(), screenY.toFloat())

        val (worldx, worldy) = Core.input.mouseWorld(screenX.toFloat(), screenY.toFloat())

        //ignore off-screen taps
        if (cursor == null || Core.scene.hasMouse(screenX.toFloat(), screenY.toFloat())) return false

        //only begin selecting if the tapped block is a plan
        selecting = hasPlan(cursor) && !commandMode

        //call tap events
        if (pointer == 0 && !selecting) {
            if (schematicMode && block == null) {
                mode = if (rebuildMode) PlaceMode.rebuildSelect else PlaceMode.schematicSelect

                //engage schematic selection mode
                val tileX = cursor.x.toInt()
                val tileY = cursor.y.toInt()
                lineTiles = cursor.tiles
                lineStartX = tileX
                lineStartY = tileY
                lastLineX = tileX
                lastLineY = tileY
            } else if (!tryTapPlayer(worldx, worldy) && Core.settings.getBool("keyboard")) {
                //shoot on touch down when in keyboard mode
                Vars.player.shooting = !Vars.state.isEditor()
            }
        }

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: KeyCode): Boolean {
        lastZoom = Vars.renderer.getScale()

        if (!Core.input.isTouched()) {
            down = false
        }

        manualShooting = false
        selecting = false

        val tile = tileAt(screenX.toFloat(), screenY.toFloat())

        //place down a line if in line mode
        if (lineMode) {

            val (tileX, tileY) = getTilePos(lineTiles, screenX.toFloat(), screenY.toFloat())

            if (mode == PlaceMode.placing && isPlacing()) {
                flushSelectPlans(linePlans)
                Events.fire(LineConfirmEvent())
            } else if (mode == PlaceMode.breaking) {
                removeSelection(lineStartX, lineStartY, tileX.toInt(), tileY.toInt(), true)
            }

            lineMode = false
        } else if (mode == PlaceMode.schematicSelect) {
            selectPlans.clear()
            lastSchematic = Vars.schematics.create(lineStartX, lineStartY, lastLineX, lastLineY, currentTiles!!)
            useSchematic(lastSchematic)
            if (selectPlans.isEmpty()) {
                lastSchematic = null
            }
            schematicMode = false
            mode = PlaceMode.none
        } else if (mode == PlaceMode.rebuildSelect) {
            rebuildArea(lineStartX, lineStartY, lastLineX, lastLineY)
            mode = PlaceMode.none
        } else if (!Vars.player.dead()) {

            tryDropItems(
                if (tile == null) null else tile.build,
                Core.input.mouseWorld(screenX.toFloat(), screenY.toFloat()).x,
                Core.input.mouseWorld(screenX.toFloat(), screenY.toFloat()).y
            )
        }

        //select some units
        selectUnitsRect()

        return false
    }

    override fun longPress(x: Float, y: Float): Boolean {
        if (Vars.state.isMenu() || Vars.player.dead() || locked()) return false

        //get tile on cursor
        val cursor = tileAt(x, y)

        if (Core.scene.hasMouse(x, y) || schematicMode) return false

        //handle long tap when player isn't building
        if (mode == PlaceMode.none) {
            val pos = Core.input.mouseWorld(x, y)

            if (commandMode) {
                //long press begins rect selection.

                commandRect = true
                commandRectX = Core.input.mouseWorldX()
                commandRectY = Core.input.mouseWorldY()
            } else {
                val pay = Vars.player.unit()
                if (pay is Payloadc) {
                    val target = Units.closest(
                        Vars.player.team(),
                        pos.x,
                        pos.y,
                        8f
                    )
                    { u ->
                        u!!.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(
                            pos,
                            u.hitSize + 8f
                        )
                    }
                    if (target != null) {
                        payloadTarget = target
                    } else {
                        val build = Vars.world.buildWorld(pos.x, pos.y)

                        if (build != null && build.team === Vars.player.team() && (pay.canPickup(build) || build.getPayload() != null && pay.canPickupPayload(
                                build.getPayload()
                            ))
                        ) {
                            payloadTarget = build
                        } else if (pay.hasPayload()) {
                            //drop off at position
                            payloadTarget = Vec2(pos)
                        } else {
                            manualShooting = true
                            this.target = null
                        }
                    }
                } else {
                    manualShooting = true
                    this.target = null
                }
            }

            if (!Vars.state.isPaused()) Fx.select.at(pos)
        } else {
            //ignore off-screen taps

            if (cursor == null) return false

            //remove plan if it's there
            //long pressing enables line mode otherwise
            lineStartX = cursor.x.toInt()
            lineStartY = cursor.y.toInt()
            lastLineX = cursor.x.toInt()
            lastLineY = cursor.y.toInt()
            lineTiles = cursor.tiles
            lineMode = true

            if (mode == PlaceMode.breaking) {
                if (!Vars.state.isPaused()) Fx.tapBlock.at(cursor.absWorldx(), cursor.absWorldy(), 1f)
            } else if (block != null) {
                updateLine(lineStartX, lineStartY, cursor.x.toInt(), cursor.y.toInt())
                if (!Vars.state.isPaused()) Fx.tapBlock.at(
                    //TODO Block Offset
                    cursor.absWorldx(),
                    cursor.absWorldy(),
                    block.size.toFloat()
                )
            }
        }

        return false
    }

    override fun tap(x: Float, y: Float, count: Int, button: KeyCode): Boolean {
        if (Vars.state.isMenu() || lineMode || locked()) return false

        val (worldx, worldy) = Core.input.mouseWorld(x, y)

        //get tile on cursor
        val cursor = tileAt(x, y)

        //ignore off-screen taps
        if (cursor == null || Core.scene.hasMouse(x, y)) return false

        Call.tileTap(Vars.player, cursor)

        val linked = if (cursor.build == null) cursor else cursor.build.tile

        if (!Vars.player.dead()) {
            checkTargets(worldx, worldy, cursor)
        }

        //remove if plan present
        if (hasPlan(cursor) && !commandMode) {
            removePlan(getPlan(cursor)!!)
        } else if (mode == PlaceMode.placing && isPlacing() && validPlace(
                cursor.x.toInt(),
                cursor.y.toInt(),
                cursor.tiles,
                block,
                rotation
            ) && !checkOverlapPlacement(cursor.x.toInt(), cursor.y.toInt(), cursor.tiles, block)
        ) {
            //add to selection queue if it's a valid place position
            selectPlans.add(
                BuildPlan(
                    cursor.x.toInt(),
                    cursor.y.toInt(),
                    cursor.tiles,
                    rotation,
                    block,
                    block.nextConfig()
                ).also { lastPlaced = it })
            block.onNewPlan(lastPlaced)
        } else if (mode == PlaceMode.breaking && validBreak(
                linked.x.toInt(),
                linked.y.toInt(),
                linked.tiles
            ) && !hasPlan(linked)
        ) {
            //add to selection queue if it's a valid BREAK position
            selectPlans.add(BuildPlan(linked.x.toInt(), linked.y.toInt(), cursor.tiles))
        } else if ((commandMode && selectedUnits.size > 0) || commandBuildings.size > 0) {
            //handle selecting units with command mode
            commandTap(x, y, queueCommandMode)
        } else if (commandMode) {
            tapCommandUnit()
        } else {
            //control units
            if (count == 2) {
                //reset payload target
                payloadTarget = null

                //control a unit/block detected on first tap of double-tap
                if (unitTapped != null && Vars.state.rules.possessionAllowed && unitTapped!!.isAI() && unitTapped!!.team === Vars.player.team() && !unitTapped!!.dead && unitTapped!!.playerControllable()) {
                    Call.unitControl(Vars.player, unitTapped)
                    recentRespawnTimer = 1f
                } else if (buildingTapped != null && Vars.state.rules.possessionAllowed) {
                    Call.buildingControlSelect(Vars.player, buildingTapped)
                    recentRespawnTimer = 1f
                } else if (!checkConfigTap() && !tryBeginMine(cursor)) {
                    tileTapped(linked.build)
                }
                return false
            }

            unitTapped = selectedUnit()
            buildingTapped = selectedControlBuild()

            //prevent mining if placing/breaking blocks
            if (!tryRepairDerelict(cursor) && !tryStopMine() && !canTapPlayer(
                    worldx,
                    worldy
                ) && !checkConfigTap() && !tileTapped(linked.build) && mode == PlaceMode.none && !Core.settings.getBool(
                    "doubletapmine"
                )
            ) {
                tryBeginMine(cursor)
            }
        }

        return false
    }

    override fun updateState() {
        super.updateState()

        if (Vars.state.isMenu()) {
            selectPlans.clear()
            removals.clear()
            mode = PlaceMode.none
            manualShooting = false
            payloadTarget = null
        }
    }

    override fun update() {
        super.update()

        val locked = locked()

        if (!commandMode) {
            queueCommandMode = false
        } else {
            mode = PlaceMode.none
            schematicMode = false
        }

        //cannot rebuild and place at the same time
        if (block != null) {
            rebuildMode = false
        }

        if (Vars.player.dead()) {
            mode = PlaceMode.none
            manualShooting = false
            payloadTarget = null
        }

        if (locked || block != null || Core.scene.hasField() || hasSchematic()) {
            commandMode = false
        }

        //validate commanding units
        selectedUnits.removeAll { u: Unit -> !u.allowCommand() || !u.isValid() || u.team !== Vars.player.team() }

        if (!commandMode) {
            commandBuildings.clear()
            selectedUnits.clear()
        }

        //zoom camera
        if (!locked && !Core.scene.hasKeyboard() && !Core.scene.hasScroll() && abs(Core.input.axisTap(Binding.zoom)) > 0 && !Core.input.keyDown(
                Binding.rotatePlaced
            ) && (Core.input.keyDown(Binding.diagonalPlacement) || ((!Vars.player.isBuilder() || !isPlacing() || !block.rotate) && selectPlans.isEmpty()))
        ) {
            Vars.renderer.scaleCamera(Core.input.axisTap(Binding.zoom))
        }

        if (!Core.settings.getBool("keyboard") && !locked && !Core.scene.hasKeyboard()) {
            //move camera around
            val camSpeed = 6f
            val delta = Tmp.v1.setZero().add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor().scl(
                Time.delta * camSpeed
            )
            Core.camera.position.add(delta)
            if (!delta.isZero()) {
                spectating = null
            }
        }

        if (Core.settings.getBool("keyboard")) {
            if (Core.input.keyRelease(Binding.select)) {
                Vars.player.shooting = false
            }

            if (Vars.player.shooting && !canShoot()) {
                Vars.player.shooting = false
            }
        }

        if (!Vars.player.dead() && !Vars.state.isPaused() && !locked) {
            updateMovement(Vars.player.unit())
        }

        //reset state when not placing
        if (mode == PlaceMode.none) {
            lineMode = false
        }

        if (lineMode && mode == PlaceMode.placing && block == null) {
            lineMode = false
        }

        //if there is no mode and there's a recipe, switch to placing
        if (block != null && mode == PlaceMode.none) {
            mode = PlaceMode.placing
        }

        if (block == null && mode == PlaceMode.placing) {
            mode = PlaceMode.none
        }

        //stop schematic when in block mode
        if (block != null) {
            schematicMode = false
        }

        //stop select when not in schematic mode
        if (!schematicMode && (mode == PlaceMode.schematicSelect || mode == PlaceMode.rebuildSelect)) {
            mode = PlaceMode.none
        }

        if (!rebuildMode && mode == PlaceMode.rebuildSelect) {
            mode = PlaceMode.none
        }

        if (mode == PlaceMode.schematicSelect || mode == PlaceMode.rebuildSelect) {
            val (lx, ly) = rawTilePos()
            lastLineX = lx.toInt()
            lastLineY = ly.toInt()
            autoPan()
        }

        //automatically switch to placing after a new recipe is selected
        if (lastBlock !== block && mode == PlaceMode.breaking && block != null) {
            mode = PlaceMode.placing
            lastBlock = block
        }

        if (lineMode) {
            lineScale = Mathf.lerpDelta(lineScale, 1f, 0.1f)

            //When in line mode, pan when near screen edges automatically
            if (Core.input.isTouched(0)) {
                autoPan()
            }

            val (lxf, lyf) = rawTilePos()
            val lx = lxf.toInt()
            val ly = lyf.toInt()

            if ((lastLineX != lx || lastLineY != ly) && isPlacing()) {
                lastLineX = lx
                lastLineY = ly
                updateLine(lineStartX, lineStartY, lx, ly)
            }
        } else {
            linePlans.clear()
            lineScale = 0f
        }

        //remove place plans that have disappeared
        var i = removals.size - 1
        while (i >= 0) {
            if (removals.get(i).animScale <= 0.0001f) {
                removals.remove(i)
                i--
            }
            i--
        }

        if (Vars.player.shooting && !Vars.player.dead() && (Vars.player.unit().activelyBuilding() || Vars.player.unit()
                .mining())
        ) {
            Vars.player.shooting = false
        }
    }

    protected fun autoPan() {
        val screenX = Core.input.mouseX().toFloat()
        val screenY = Core.input.mouseY().toFloat()

        var panX = 0f
        var panY = 0f

        if (screenX <= edgePan) {
            panX = -(edgePan - screenX)
        }

        if (screenX >= Core.graphics.getWidth() - edgePan) {
            panX = (screenX - Core.graphics.getWidth()) + edgePan
        }

        if (screenY <= edgePan) {
            panY = -(edgePan - screenY)
        }

        if (screenY >= Core.graphics.getHeight() - edgePan) {
            panY = (screenY - Core.graphics.getHeight()) + edgePan
        }

        vector.set(panX, panY).scl((Core.camera.width) / Core.graphics.getWidth())
        vector.limit(maxPanSpeed)

        //pan view
        Core.camera.position.x += vector.x
        Core.camera.position.y += vector.y
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        var deltaX = deltaX
        var deltaY = deltaY
        if (Core.scene == null || Core.scene.hasDialog() || Core.settings.getBool("keyboard") || locked() || commandRect) return false

        val scale = Core.camera.width / Core.graphics.getWidth()
        deltaX *= scale
        deltaY *= scale

        //can't pan in line mode with one finger or while dropping items!
        if ((lineMode && !Core.input.isTouched(1)) || droppingItem || schematicMode) {
            return false
        }

        //do not pan with manual shooting enabled
        if (!down || manualShooting) return false

        if (selecting) { //pan all plans
            shiftDeltaX += deltaX
            shiftDeltaY += deltaY

            val shiftedX = (shiftDeltaX / Vars.tilesize).toInt()
            val shiftedY = (shiftDeltaY / Vars.tilesize).toInt()

            if (abs(shiftedX) > 0 || abs(shiftedY) > 0) {
                for (plan in selectPlans) {
                    if (plan.breaking) continue  //don't shift removal plans

                    plan.x += shiftedX
                    plan.y += shiftedY
                }

                shiftDeltaX %= Vars.tilesize.toFloat()
                shiftDeltaY %= Vars.tilesize.toFloat()
            }
        } else {
            //pan player
            Core.camera.position.x -= deltaX
            Core.camera.position.y -= deltaY
            spectating = null
        }

        Core.camera.position.clamp(
            -Core.camera.width / 4f,
            -Core.camera.height / 4f,
            Vars.world.unitWidth() + Core.camera.width / 4f,
            Vars.world.unitHeight() + Core.camera.height / 4f
        )

        return false
    }

    override fun panStop(x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
        shiftDeltaY = 0f
        shiftDeltaX = shiftDeltaY
        return false
    }

    override fun zoom(initialDistance: Float, distance: Float): Boolean {
        if (Core.settings.getBool("keyboard")) return false
        if (lastZoom < 0) {
            lastZoom = Vars.renderer.getScale()
        }

        Vars.renderer.setScale(distance / initialDistance * lastZoom)
        return true
    }

    //endregion
    //region movement
    protected fun updateMovement(unit: Unit) {
        val rect = Tmp.r3

        val type = unit.type
        if (type == null) return

        val omni = unit.type.omniMovement
        val allowHealing = type.canHeal
        val targetL = target
        val validHealTarget =
            allowHealing && targetL is Building && targetL.isValid() && targetL.team() === unit.team && targetL.damaged() && targetL.within(
                unit,
                type.range
            )
        val boosted = (unit is Mechc && unit.isFlying())
        //reset target if:
        // - in the editor, or...
        // - it's both an invalid standard target and an invalid heal target
        if ((Units.invalidateTarget(target, unit, type.range) && !validHealTarget) || Vars.state.isEditor()) {
            target = null
        }

        targetPos.set(Core.camera.position)

        var attractDst = 15f
        val speed = unit.speed()
        val range = if (unit.hasWeapons()) unit.range() else 0f
        val mouseAngle = unit.angleTo(unit.aimX(), unit.aimY())
        val aimCursor = omni && Vars.player.shooting && type.hasWeapons() && !boosted && type.faceTarget

        if (aimCursor) {
            unit.lookAt(mouseAngle)
        } else {
            unit.lookAt(unit.prefRotation())
        }

        //validate payload, if it's a destroyed unit/building, remove it
        val payloadTargetL = payloadTarget
        if (payloadTargetL is Healthc && !payloadTargetL.isValid()) {
            payloadTarget = null
        }

        if (payloadTarget != null && unit is Payloadc) {
            targetPos.set(payloadTarget)
            attractDst = 0f

            val payloadTargetL = payloadTarget
            if (unit.within(payloadTarget, 3f * Time.delta)) {
                if (payloadTarget is Vec2 && unit.hasPayload()) {
                    //vec -> dropping something
                    tryDropPayload()
                } else if (payloadTargetL is Building && payloadTargetL.team === unit.team) {
                    //building -> picking building up
                    Call.requestBuildPayload(Vars.player, payloadTargetL)
                } else if (payloadTargetL is Unit && unit.canPickup(payloadTargetL)) {
                    //unit -> picking unit up
                    Call.requestUnitPayload(Vars.player, payloadTargetL)
                }

                payloadTarget = null
            }
        } else {
            payloadTarget = null
        }

        movement.set(targetPos).sub(Vars.player).limit(speed)
        movement.setAngle(Mathf.slerp(movement.angle(), unit.vel.angle(), 0.05f))

        if (Vars.player.within(targetPos, attractDst)) {
            movement.setZero()
            unit.vel.approachDelta(Vec2.ZERO, unit.speed() * type.accel / 2f)
        }

        unit.hitbox(rect)
        rect.grow(4f)

        Vars.player.boosting = Vars.collisions.overlapsTile(
            rect,
            { x, y -> EntityCollisions.solid(x, y) }) || !unit.within(targetPos, 85f)

        unit.movePref(movement)

        //update shooting if not building + not mining
        if (!unit.activelyBuilding() && unit.mineTile == null && !Vars.state.isEditor()) {
            //autofire targeting

            if (manualShooting) {
                Vars.player.shooting = !boosted
                unit.aim(
                    Core.input.mouseWorldX().also { Vars.player.mouseX = it },
                    Core.input.mouseWorldY().also { Vars.player.mouseY = it })
            } else if (target == null) {
                Vars.player.shooting = false
                if (Core.settings.getBool("autotarget") && !((Vars.player.unit() as? BlockUnitUnit)?.takeIf { it.tile() is ControlBlock }
                        ?.let { !(it.tile() as ControlBlock).shouldAutoTarget() } ?: false)) {
                    if (unit.type.canAttack) {
                        target = Units.closestTarget(
                            unit.team,
                            unit.x,
                            unit.y,
                            range,
                            { u -> u.checkTarget(type.targetAir, type.targetGround) },
                            { u -> type.targetGround && type.targetBuildingsMobile })
                    }

                    if (allowHealing && target == null) {
                        target =
                            Geometry.findClosest(unit.x, unit.y, Vars.indexer.getDamaged(Vars.player.team()))
                        if (target != null && !unit.within(target, range)) {
                            target = null
                        }
                    }
                }

                //when not shooting, aim at mouse cursor
                //this may be a bad idea, aiming for a point far in front could work better, test it out
                unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY())
            } else {
                val intercept =
                    if (Vars.player.unit().type.weapons.contains { w -> w.predictTarget }) Predict.intercept(
                        unit,
                        target,
                        type.weapons.first().bullet
                    ) else Tmp.v1.set(target)

                Vars.player.mouseX = intercept.x
                Vars.player.mouseY = intercept.y
                Vars.player.shooting = !boosted

                unit.aim(Vars.player.mouseX, Vars.player.mouseY)
            }
        }

        unit.controlWeapons(Vars.player.shooting && !boosted)
    } //endregion

    companion object {
        /** Maximum speed the player can pan.  */
        private const val maxPanSpeed = 1.3f
    }
}
