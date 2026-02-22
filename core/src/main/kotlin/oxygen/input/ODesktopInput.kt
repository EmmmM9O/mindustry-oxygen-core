package oxygen.input

import arc.*
import arc.Graphics.Cursor.SystemCursor
import arc.func.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.input.*
import arc.math.*
import arc.math.geom.*
import arc.scene.*
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.core.*
import mindustry.entities.units.*
import mindustry.game.*
import mindustry.game.EventType.LineConfirmEvent
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.graphics.*
import mindustry.input.*
import mindustry.ui.*
import mindustry.world.*
import oxygen.*
import oxygen.math.geom.*
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.also
import kotlin.math.*

class ODesktopInput : OInputHandler() {
    var movement: Vec2 = Vec2()

    /** Current cursor type.  */
    var cursorType: Graphics.Cursor = SystemCursor.arrow

    /** Position where the player started dragging a line.  */
    var selectX: Int = -1
    var selectY: Int = -1
    var schemX: Int = -1
    var schemY: Int = -1

    /** Last known line positions. */
    var lastLineX: Int = 0
    var lastLineY: Int = 0
    var schematicX: Int = 0
    var schematicY: Int = 0

    /** Whether selecting mode is active.  */
    var mode: PlaceMode? = null

    /** Animation scale for line.  */
    var selectScale: Float = 0f

    /** Selected build plan for movement.  */
    @Nullable
    var splan: BuildPlan? = null

    /** Whether player is currently deleting removal plans.  */
    var deleting: Boolean = false
    var shouldShoot: Boolean = false
    var panning: Boolean = false
    var movedPlan: Boolean = false

    /** Mouse pan speed.  */
    var panScale: Float = 0.005f
    var panSpeed: Float = 4.5f
    var panBoostSpeed: Float = 15f

    /** Delta time between consecutive clicks.  */
    var selectMillis: Long = 0

    /** Previously selected tile.  */
    var prevSelected: Tile? = null

    /** Most recently selected control group by index  */
    var lastCtrlGroup: Int = 0

    /** Time of most recent control group selection  */
    var lastCtrlGroupSelectMillis: Long = 0

    /** Time of most recent payload pickup/drop key press */
    var lastPayloadKeyTapMillis: Long = 0

    /** Time of most recent payload pickup/drop key hold */
    var lastPayloadKeyHoldMillis: Long = 0

    private var buildPlanMouseOffsetX = 0f
    private var buildPlanMouseOffsetY = 0f
    private var changedCursor = false
    private val pressedCommandRect = false

    fun showHint(): Boolean {
        return Vars.ui.hudfrag.shown && Core.settings.getBool("hints") && selectPlans.isEmpty() && !Vars.player.dead() &&
                (!isBuilding && !Core.settings.getBool("buildautopause") || Vars.player.unit()
                    .isBuilding() || !Vars.player.dead() && !Vars.player.unit().spawnedByCore())
    }

    override fun buildUI(group: Group) {
        //building and respawn hints
        group.fill { t ->
            t.color.a = 0f
            t.visible {
                (Mathf.lerpDelta(t.color.a, Mathf.num(showHint()).toFloat(), 0.15f)
                    .also { t.color.a = it }) > 0.001f
            }
            t.bottom()
            t.table(Styles.black6) { b ->
                val str = StringBuilder()
                b.defaults().left()
                b.label {
                    if (!showHint()) return@label str
                    str.setLength(0)
                    if (!isBuilding && !Core.settings.getBool("buildautopause") && !Vars.player.unit().isBuilding()) {
                        str.append(Core.bundle.format("enablebuilding", Binding.pauseBuilding.value.key.toString()))
                    } else if (Vars.player.unit().isBuilding()) {
                        str.append(
                            Core.bundle.format(
                                if (isBuilding) "pausebuilding" else "resumebuilding",
                                Binding.pauseBuilding.value.key.toString()
                            )
                        )
                            .append("\n")
                            .append(Core.bundle.format("cancelbuilding", Binding.clearBuilding.value.key.toString()))
                            .append("\n")
                            .append(Core.bundle.format("selectschematic", Binding.schematicSelect.value.key.toString()))
                    }
                    if (!Vars.player.dead() && !Vars.player.unit().spawnedByCore()) {
                        str.append(if (str.length != 0) "\n" else "")
                            .append(Core.bundle.format("respawn", Binding.respawn.value.key.toString()))
                    }
                    str
                }.style(Styles.outlineLabel)
            }.margin(10f)
        }

        //schematic controls
        group.fill { t ->
            t.visible { Vars.ui.hudfrag.shown && lastSchematic != null && !selectPlans.isEmpty() }
            t.bottom()
            t.table(Styles.black6) { b ->
                b.defaults().left()
                b.label {
                    Core.bundle.format(
                        "schematic.flip",
                        Binding.schematicFlipX.value.key.toString(),
                        Binding.schematicFlipY.value.key.toString()
                    )
                }.style(Styles.outlineLabel).visible { Core.settings.getBool("hints") }
                b.row()
                b.table { a ->
                    a.button("@schematic.add", Icon.save) { this.showSchematicSave() }.colspan(2)
                        .size(250f, 50f)
                        .disabled { f -> lastSchematic == null || lastSchematic.file != null }
                }
            }.margin(6f)
        }
    }

    override fun drawTop() {
        if (cursorType !== SystemCursor.arrow && Core.scene.hasMouse()) {
            Core.graphics.cursor(SystemCursor.arrow.also { cursorType = it })
        }

        Lines.stroke(1f)
        val tiles = lineTiles
        drawTiles = tiles
        val (lx, ly) = rawTilePos(tiles)
        val cursorX = lx.toInt()
        val cursorY = ly.toInt()
        //draw break selection

        if (tiles.craft != null) Oxygen.renderer.drawTilesFunc(tiles.craft) {
            if (mode == PlaceMode.breaking) {
                drawBreakSelection(
                    selectX,
                    selectY,
                    cursorX,
                    cursorY,
                    if (!(Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1)) InputHandler.maxLength else Vars.maxSchematicSize,
                    false
                )
            }

            if (!Core.scene.hasKeyboard() && mode != PlaceMode.breaking) {
                if (Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1) {
                    drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize)
                } else if (Core.input.keyDown(Binding.rebuildSelect)) {
                    drawRebuildSelection(schemX, schemY, cursorX, cursorY)
                }
            }
        }

        Draw.reset()
    }

    override fun drawBottom() {
        val splan = splan
        //draw plan being moved
        if (splan != null) {
            val tiles = splan.tiles
            drawTiles = tiles
            val (lx, ly) = rawTilePos(tiles)
            val cursorX = lx.toInt()
            val cursorY = ly.toInt()
            Oxygen.renderer.drawTilesFunc(tiles.craft) {
                val valid = validPlace(splan.x, splan.y, tiles, splan.block, splan.rotation, splan)
                if (splan.block.rotate && splan.block.drawArrow) {
                    drawArrow(splan.block, splan.x, splan.y, splan.rotation, valid)
                }

                splan.block.drawPlan(splan, allPlans(), valid)

                drawSelected(
                    splan.x,
                    splan.y,
                    splan.block,
                    if (getPlan(splan.x, splan.y, tiles, splan.block.size, splan) != null) Pal.remove else Pal.accent
                )
            }
        }

        val cursor = tileAtF(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
        //draw hover plans
        if (mode == PlaceMode.none && !isPlacing() && cursor != null) {
            val plan = getPlan(cursor.x.toInt(), cursor.y.toInt(), cursor.tiles)
            if (cursor.tiles.craft != null) Oxygen.renderer.drawTilesFunc(cursor.tiles.craft) {
                if (plan != null) {
                    drawSelected(plan.x, plan.y, if (plan.breaking) plan.tile().block() else plan.block, Pal.accent)
                }
            }
        }

        for (entry in genTilesPlans(selectPlans)) {
            val tiles = entry.key
            val plans = entry.value
            drawTiles = tiles
            if (tiles.craft != null) Oxygen.renderer.drawTilesFunc(tiles.craft) {
                for (i in 0..1) {
                    for (plan in plans) {
                        if (i == 0) {
                            //draw schematic plans
                            plan.animScale = 1f
                            drawPlan(plan)
                        } else {
                            //draw schematic plans - over version, cached results
                            drawOverPlan(plan, plan.cachedValid)
                        }
                    }
                }
            }
        }

        if (Vars.player.isBuilder()) {
            //draw things that may be placed soon
            if (mode == PlaceMode.placing && block != null) {
                val tiles = lineTiles
                drawTiles = tiles
                Oxygen.renderer.drawTilesFunc(tiles.craft) {
                    for (i in 0..<linePlans.size) {
                        val plan = linePlans.get(i)
                        if (i == linePlans.size - 1 && plan.block.rotate && plan.block.drawArrow) {
                            drawArrow(block, plan.x, plan.y, plan.rotation)
                        }
                        drawPlan(linePlans.get(i))
                    }
                    linePlans.each { plan -> this.drawOverPlan(plan) }
                }
            } else if (isPlacing() && cursor != null) {
                val tiles = cursor.tiles
                val cursorX = cursor.x.toInt()
                val cursorY = cursor.y.toInt()
                Oxygen.renderer.drawTilesFunc(tiles.craft) {
                    val rot = if (block == null) rotation else block.planRotation(rotation)
                    if (block.rotate && block.drawArrow) {
                        drawArrow(block, cursorX, cursorY, rot)
                    }
                    Draw.color()
                    val valid = validPlace(cursorX, cursorY, tiles, block, rot)
                    drawPlan(cursorX, cursorY, tiles, block, rot)
                    block.drawPlace(cursorX, cursorY, rot, valid)

                    if (block.saveConfig) {
                        Draw.mixcol(
                            if (!valid) Pal.breakInvalid else Color.white, (if (!valid) 0.4f else 0.24f) + Mathf.absin(
                                Time.globalTime, 6f, 0.28f
                            )
                        )
                        bplan.set(cursorX, cursorY, rot, block)
                        bplan.config = block.lastConfig
                        block.drawPlanConfig(bplan, allPlans())
                        bplan.config = null
                        Draw.reset()
                    }

                    drawOverlapCheck(block, cursorX, cursorY, valid)
                }
            }
        }

        Draw.reset()
    }

    override fun update() {
        super.update()

        if (Vars.net.active() && Core.input.keyTap(Binding.playerList) && (Core.scene.getKeyboardFocus() == null || Core.scene.getKeyboardFocus()
                .isDescendantOf(
                    Vars.ui.listfrag.content
                ) || Core.scene.getKeyboardFocus().isDescendantOf(Vars.ui.minimapfrag.elem))
        ) {
            Vars.ui.listfrag.toggle()
        }

        val locked = locked()
        var panCam = false
        val camSpeed = (if (!Core.input.keyDown(Binding.boost)) panSpeed else panBoostSpeed) * Time.delta
        var detached = Core.settings.getBool("detach-camera", false)

        if (!Core.scene.hasField() && !Core.scene.hasDialog()) {
            if (Core.input.keyTap(Binding.debugHitboxes)) {
                Vars.drawDebugHitboxes = !Vars.drawDebugHitboxes
            }

            if (Core.input.keyTap(Binding.detachCamera)) {
                Core.settings.put("detach-camera", (!detached).also { detached = it })
                if (!detached) {
                    panning = false
                }
                spectating = null
            }

            if (Core.input.keyDown(Binding.pan)) {
                panCam = true
                panning = true
                spectating = null
            }

            if ((abs(Core.input.axis(Binding.moveX)) > 0 || abs(Core.input.axis(Binding.moveY)) > 0 || Core.input.keyDown(
                    Binding.mouseMove
                ))
            ) {
                panning = false
                spectating = null
            }
        }

        panning = panning or detached


        if (!locked) {
            if (((Vars.player.dead() || Vars.state.isPaused() || detached) && !Vars.ui.chatfrag.shown()) && !Core.scene.hasField() && !Core.scene.hasDialog()) {
                if (Core.input.keyDown(Binding.mouseMove)) {
                    panCam = true
                }

                Core.camera.position.add(
                    Tmp.v1.setZero().add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor()
                        .scl(camSpeed)
                )
            } else if ((!Vars.player.dead() || spectating != null) && !panning) {
                //TODO do not pan
                val corePanTeam = if (Vars.state.won) Vars.state.rules.waveTeam else Vars.player.team()
                val coreTarget: Position? =
                    if (Vars.state.gameOver && !Vars.state.rules.pvp && corePanTeam.data().lastCore != null) corePanTeam.data().lastCore else null
                val panTarget =
                    if (coreTarget != null) coreTarget else if (spectating != null) spectating else Vars.player

                Core.camera.position.lerpDelta(panTarget, if (Core.settings.getBool("smoothcamera")) 0.08f else 1f)
            }

            if (panCam) {
                Core.camera.position.x += Mathf.clamp(
                    (Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale,
                    -1f,
                    1f
                ) * camSpeed
                Core.camera.position.y += Mathf.clamp(
                    (Core.input.mouseY() - Core.graphics.getHeight() / 2f) * panScale,
                    -1f,
                    1f
                ) * camSpeed
            }
        }

        shouldShoot = !Core.scene.hasMouse() && !locked && !Vars.state.isEditor()

        if (!locked && block == null && !Core.scene.hasField() && !Core.scene.hasDialog() &&  //disable command mode when player unit can boost and command mode binding is the same
            !(!Vars.player.dead() && Vars.player.unit().type.canBoost && Binding.commandMode.value.key == Binding.boost.value.key)
        ) {
            if (Core.settings.getBool("commandmodehold")) {
                commandMode = Core.input.keyDown(Binding.commandMode)
            } else if (Core.input.keyTap(Binding.commandMode)) {
                commandMode = !commandMode
            }
        } else {
            commandMode = false
        }

        //validate commanding units
        selectedUnits.removeAll { u -> !u.allowCommand() || !u.isValid() || u.team !== Vars.player.team() }

        if (commandMode && !Core.scene.hasField() && !Core.scene.hasDialog()) {
            if (Core.input.keyTap(Binding.selectAllUnits)) {
                selectedUnits.clear()
                commandBuildings.clear()
                if (Core.input.keyDown(Binding.selectAcrossScreen)) {
                    Core.camera.bounds(Tmp.r1)
                    selectedUnits.set(
                        selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height).removeAll
                        { u -> !u.type.controlSelectGlobal }
                    )
                } else {
                    for (unit in Vars.player.team().data().units) {
                        if (unit.isCommandable() && unit.type.controlSelectGlobal) {
                            selectedUnits.add(unit)
                        }
                    }
                }
            }

            if (Core.input.keyTap(Binding.selectAllUnitTransport)) {
                selectedUnits.clear()
                commandBuildings.clear()
                if (Core.input.keyDown(Binding.selectAcrossScreen)) {
                    Core.camera.bounds(Tmp.r1)
                    selectedUnits.set(
                        selectedCommandUnits(
                            Tmp.r1.x,
                            Tmp.r1.y,
                            Tmp.r1.width,
                            Tmp.r1.height
                        )
                        { u -> u is Payloadc }
                    )
                } else {
                    for (unit in Vars.player.team().data().units) {
                        if (unit.isCommandable() && unit is Payloadc) {
                            selectedUnits.add(unit)
                        }
                    }
                }
            }

            if (Core.input.keyTap(Binding.selectAllUnitFactories)) {
                selectedUnits.clear()
                commandBuildings.clear()
                for (build in Vars.player.team().data().buildings) {
                    if (build.isCommandable()) {
                        commandBuildings.add(build)
                    }
                }
                if (Core.input.keyDown(Binding.selectAcrossScreen)) {
                    Core.camera.bounds(Tmp.r1)
                    commandBuildings.retainAll { b ->
                        Tmp.r1.overlaps(
                            b.x - (b.hitSize() / 2),
                            b.y - (b.hitSize() / 2),
                            b.hitSize(),
                            b.hitSize()
                        )
                    }
                }
            }

            for (i in InputHandler.controlGroupBindings.indices) {
                if (Core.input.keyTap(InputHandler.controlGroupBindings[i])) {
                    //create control group if it doesn't exist yet

                    if (controlGroups[i] == null) controlGroups[i] = IntSeq()

                    val group = controlGroups[i]
                    val creating = Core.input.keyDown(Binding.createControlGroup)

                    //clear existing if making a new control group
                    //if any of the control group edit buttons are pressed take the current selection
                    if (creating) {
                        group.clear()

                        val selectedUnitIds = selectedUnits.mapInt { u -> u.id }
                        if (Core.settings.getBool("distinctcontrolgroups", true)) {
                            for (cg in controlGroups) {
                                if (cg != null) {
                                    cg.removeAll(selectedUnitIds)
                                }
                            }
                        }
                        group.addAll(selectedUnitIds)
                    }

                    //remove invalid units
                    var j = 0
                    while (j < group.size) {
                        val u = Groups.unit.getByID(group.get(j))
                        if (u == null || !u.isCommandable() || !u.isValid()) {
                            group.removeIndex(j)
                            j--
                        }
                        j++
                    }

                    //replace the selected units with the current control group
                    if (!group.isEmpty() && !creating) {
                        selectedUnits.clear()
                        commandBuildings.clear()

                        group.each { id ->
                            val unit = Groups.unit.getByID(id)
                            if (unit != null) {
                                selectedUnits.addAll(unit)
                            }
                        }

                        //double tap to center camera
                        if (lastCtrlGroup == i && Time.timeSinceMillis(lastCtrlGroupSelectMillis) < 400) {
                            var totalX = 0f
                            var totalY = 0f
                            for (unit in selectedUnits) {
                                totalX += unit.x
                                totalY += unit.y
                            }
                            panning = true
                            Core.camera.position.set(totalX / selectedUnits.size, totalY / selectedUnits.size)
                        }
                        lastCtrlGroup = i
                        lastCtrlGroupSelectMillis = Time.millis()
                    }
                }
            }
        }

        if (!Core.scene.hasMouse() && !locked && Vars.state.rules.possessionAllowed) {
            if (Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)) {
                val on = selectedUnit()
                val build = selectedControlBuild()
                if (on != null) {
                    Call.unitControl(Vars.player, on)
                    shouldShoot = false
                    recentRespawnTimer = 1f
                } else if (build != null) {
                    Call.buildingControlSelect(Vars.player, build)
                    recentRespawnTimer = 1f
                }
            }
        }

        if (!Vars.player.dead() && !Vars.state.isPaused() && !Core.scene.hasField() && !locked) {
            updateMovement(Vars.player.unit())

            if (Core.input.keyTap(Binding.respawn)) {
                controlledType = null
                recentRespawnTimer = 1f
                Call.unitClear(Vars.player)
            }
        }

        if (Vars.state.isGame() && !Core.scene.hasDialog() && !Core.scene.hasField()) {
            if (Core.input.keyTap(Binding.minimap)) Vars.ui.minimapfrag.toggle()
            if (Core.input.keyTap(Binding.planetMap) && Vars.state.isCampaign()) Vars.ui.planet.toggle()
            if (Core.input.keyTap(Binding.research) && Vars.state.isCampaign()) Vars.ui.research.toggle()
            if (Core.input.keyTap(Binding.schematicMenu)) Vars.ui.schematics.toggle()

            if (Core.input.keyTap(Binding.toggleBlockStatus)) {
                Core.settings.put("blockstatus", !Core.settings.getBool("blockstatus"))
            }

            if (Core.input.keyTap(Binding.togglePowerLines)) {
                if (Core.settings.getInt("lasersopacity") == 0) {
                    Core.settings.put("lasersopacity", Core.settings.getInt("preferredlaseropacity", 100))
                } else {
                    Core.settings.put("preferredlaseropacity", Core.settings.getInt("lasersopacity"))
                    Core.settings.put("lasersopacity", 0)
                }
            }
        }

        if (Vars.state.isMenu() || Core.scene.hasDialog()) return

        //zoom camera
        if ((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonalPlacement)) && !Vars.ui.chatfrag.shown() && !Vars.ui.consolefrag.shown() && abs(
                Core.input.axisTap(Binding.zoom)
            ) > 0 && !Core.input.keyDown(Binding.rotatePlaced) && (Core.input.keyDown(
                Binding.diagonalPlacement
            ) || (Binding.zoom.value != Binding.rotate.value) || ((!Vars.player.isBuilder() || !isPlacing() || !block.rotate) && selectPlans.isEmpty()))
        ) {
            Vars.renderer.scaleCamera(Core.input.axisTap(Binding.zoom))
        }

        if (Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()) {
            val selected = Vars.world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY())
            if (selected != null) {
                Call.tileTap(Vars.player, selected)
            }
        }

        if (Core.input.keyRelease(Binding.select) && commandRect) {
            selectUnitsRect()
        }

        if (Vars.player.dead() || locked) {
            cursorType = SystemCursor.arrow
            if (!locked) {
                pollInputNoPlayer()
            }
        } else {
            pollInputPlayer()
        }

        if (Core.input.keyRelease(Binding.select)) {
            Vars.player.shooting = false
        }

        if (!Core.scene.hasMouse() && !Vars.ui.minimapfrag.shown()) {
            Core.graphics.cursor(cursorType)
            changedCursor = cursorType !== SystemCursor.arrow
        } else {
            cursorType = SystemCursor.arrow
            if (changedCursor) {
                Core.graphics.cursor(SystemCursor.arrow)
                changedCursor = false
            }
        }
    }

    override fun useSchematic(schem: Schematic?, checkHidden: Boolean) {
        block = null
        val tile = tileAtF(getMouseX(), getMouseY())
        if (tile == null) return
        schematicX = tile.x.toInt()
        schematicY = tile.y.toInt()

        selectPlans.clear()
        selectPlans.addAll(Vars.schematics.toPlans(schem, schematicX, schematicY, tile.tiles, checkHidden))
        mode = PlaceMode.none
    }

    override fun isBreaking(): Boolean {
        return mode == PlaceMode.breaking
    }

    override fun buildPlacementUI(table: Table) {
        table.left().margin(0f).defaults().size(48f).left()

        table.button(Icon.paste, Styles.clearNonei) {
            Vars.ui.schematics.show()
        }.tooltip("@schematics")

        table.button(Icon.book, Styles.clearNonei) {
            Vars.ui.database.show()
        }.tooltip("@database")

        table.button(Icon.tree, Styles.clearNonei) {
            Vars.ui.research.show()
        }.visible { Vars.state.isCampaign() }.tooltip("@research")

        table.button(Icon.map, Styles.clearNonei) {
            Vars.ui.planet.show()
        }.visible { Vars.state.isCampaign() }.tooltip("@planetmap")
    }

    fun pollInputNoPlayer() {
        if (Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()) {
            tappedOne = false

            val selected = tileAt(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())

            if (commandMode) {
                commandRect = true
                commandRectX = Core.input.mouseWorldX()
                commandRectY = Core.input.mouseWorldY()
            } else if (selected != null) {
                tileTapped(selected.build)
            }
        }
    }

    //player input: for controlling the player unit (will crash if the unit is not present)
    fun pollInputPlayer() {
        if (Core.scene.hasField()) return

        val selected = tileAt(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
        val tiles = currentTiles ?: Vars.world.tiles
        val cursorX = selected?.x?.toInt() ?: tileX(Core.input.mouseX().toFloat())
        val cursorY = selected?.y?.toInt() ?: tileY(Core.input.mouseY().toFloat())
        val rawPos = getPos(tiles, Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
        val rawCursorX = World.toTile(rawPos.x)
        val rawCursorY = World.toTile(rawPos.y)

        //automatically pause building if the current build queue is empty
        if (Core.settings.getBool("buildautopause") && isBuilding && !Vars.player.unit().isBuilding()) {
            isBuilding = false
            buildWasAutoPaused = true
        }

        if (!selectPlans.isEmpty()) {
            val shiftX = rawCursorX - schematicX
            val shiftY = rawCursorY - schematicY

            selectPlans.each { s ->
                s.x += shiftX
                s.y += shiftY
            }

            schematicX += shiftX
            schematicY += shiftY
        }

        if (Core.input.keyTap(Binding.deselect) && !Vars.ui.minimapfrag.shown() && !isPlacing() && Vars.player.unit().plans.isEmpty() && !commandMode) {
            Vars.player.unit().mineTile = null
        }

        if (Core.input.keyTap(Binding.clearBuilding) && !Vars.player.dead()) {
            Vars.player.unit().clearBuilding()
        }

        if ((Core.input.keyTap(Binding.schematicSelect) || Core.input.keyTap(Binding.rebuildSelect)) && !Core.scene.hasKeyboard() && mode != PlaceMode.breaking) {
            lineTiles = tiles
            schemX = rawCursorX
            schemY = rawCursorY
        }

        if (Core.input.keyTap(Binding.clearBuilding) || isPlacing()) {
            lastSchematic = null
            selectPlans.clear()
        }

        if (!Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX != -1 && schemY != -1) {
            if (Core.input.keyRelease(Binding.schematicSelect)) {
                lastSchematic = Vars.schematics.create(schemX, schemY, rawCursorX, rawCursorY, tiles)
                useSchematic(lastSchematic)
                if (selectPlans.isEmpty()) {
                    lastSchematic = null
                }
                schemX = -1
                schemY = -1
            } else if (Core.input.keyRelease(Binding.rebuildSelect)) {
                lineTiles = tiles
                rebuildArea(schemX, schemY, rawCursorX, rawCursorY)
                schemX = -1
                schemY = -1
            }
        }

        if (!selectPlans.isEmpty()) {
            if (Core.input.keyTap(Binding.schematicFlipX)) {
                flipPlans(selectPlans, true)
            }

            if (Core.input.keyTap(Binding.schematicFlipY)) {
                flipPlans(selectPlans, false)
            }
        }

        val splan = splan
        if (splan != null) {
            val vec = getPos(tiles, Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
            val x = Math.round((vec.x + buildPlanMouseOffsetX) / Vars.tilesize)
            val y = Math.round((vec.y + buildPlanMouseOffsetY) / Vars.tilesize)
            if (splan.x != x || splan.y != y || splan.tiles != tiles) {
                splan.x = x
                splan.y = y
                splan.tiles = tiles
                movedPlan = true
            }
        }

        if (block == null || mode != PlaceMode.placing) {
            linePlans.clear()
        }

        if (Core.input.keyTap(Binding.pauseBuilding)) {
            isBuilding = !isBuilding
            buildWasAutoPaused = false

            if (isBuilding) {
                Vars.player.shooting = false
            }
        }

        if (isPlacing() && mode == PlaceMode.placing && (cursorX != lastLineX || cursorY != lastLineY || Core.input.keyTap(
                Binding.diagonalPlacement
            ) || Core.input.keyRelease(Binding.diagonalPlacement))
        ) {
            lineTiles = tiles
            updateLine(selectX, selectY)
            lastLineX = cursorX
            lastLineY = cursorY
        }

        if (Core.input.keyRelease(Binding.select) && !Core.scene.hasMouse()) {
            val plan = getPlan(cursorX, cursorY, tiles)

            if (plan != null && !movedPlan) {
                //move selected to front
                val index = Vars.player.unit().plans.indexOf(plan, true)
                if (index != -1) {
                    Vars.player.unit().plans.removeIndex(index)
                    Vars.player.unit().plans.addFirst(plan)
                }
            }
        }

        if (Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()) {
            lineTiles = tiles
            tappedOne = false
            val plan = getPlan(cursorX, cursorY, tiles)

            if (Core.input.keyDown(Binding.breakBlock)) {
                mode = PlaceMode.none
            } else if (!selectPlans.isEmpty()) {
                flushPlans(selectPlans)
                movedPlan = true
            } else if (isPlacing()) {
                selectX = cursorX
                selectY = cursorY
                lastLineX = cursorX
                lastLineY = cursorY
                mode = PlaceMode.placing
                updateLine(selectX, selectY)
            } else if (plan != null && !plan.breaking && mode == PlaceMode.none && !plan.initialized && plan.progress <= 0f) {
                this.splan = plan
                movedPlan = false
                buildPlanMouseOffsetX = splan!!.x * Vars.tilesize - Core.input.mouseWorld().x
                buildPlanMouseOffsetY = splan!!.y * Vars.tilesize - Core.input.mouseWorld().y
            } else if (plan != null && plan.breaking) {
                deleting = true
            } else if (commandMode) {
                commandRect = true
                commandRectX = Core.input.mouseWorldX()
                commandRectY = Core.input.mouseWorldY()
            } else if (!checkConfigTap() && selected != null && !tryRepairDerelict(selected)) {
                //only begin shooting if there's no cursor event
                if (!tryTapPlayer(
                        Core.input.mouseWorld().x,
                        Core.input.mouseWorld().y
                    ) && !tileTapped(selected.build) && !Vars.player.unit()
                        .activelyBuilding() && !droppingItem && !(tryStopMine(selected) || (!Core.settings.getBool("doubletapmine") || selected === prevSelected && Time.timeSinceMillis(
                        selectMillis
                    ) < 500) && tryBeginMine(selected)) && !Core.scene.hasKeyboard()
                ) {
                    Vars.player.shooting = shouldShoot
                }
            } else if (!Core.scene.hasKeyboard()) { //if it's out of bounds, shooting is just fine
                Vars.player.shooting = shouldShoot
            }
            selectMillis = Time.millis()
            prevSelected = selected
        } else if (Core.input.keyTap(Binding.deselect) && isPlacing()) {
            block = null
            mode = PlaceMode.none
        } else if (Core.input.keyTap(Binding.deselect) && !selectPlans.isEmpty()) {
            selectPlans.clear()
            lastSchematic = null
        } else if (Core.input.keyTap(Binding.breakBlock) && !Core.scene.hasMouse() && Vars.player.isBuilder() && !commandMode) {
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false
            mode = PlaceMode.breaking
            selectX = cursorX
            selectY = cursorY
            schemX = rawCursorX
            schemY = rawCursorY
        }

        if (Core.input.keyDown(Binding.select) && mode == PlaceMode.none && !isPlacing() && deleting) {
            val plan = getPlan(cursorX, cursorY, tiles)
            if (plan != null && plan.breaking) {
                Vars.player.unit().plans().remove(plan)
            }
        } else {
            deleting = false
        }

        if (mode == PlaceMode.placing && block != null) {
            if (!overrideLineRotation && !Core.input.keyDown(Binding.diagonalPlacement) && (selectX != cursorX || selectY != cursorY) && (Core.input.axisTap(
                    Binding.rotate
                ).toInt() != 0)
            ) {
                rotation = (((Angles.angle(
                    selectX.toFloat(),
                    selectY.toFloat(),
                    cursorX.toFloat(),
                    cursorY.toFloat()
                ) + 45) / 90f).toInt()) % 4
                overrideLineRotation = true
            }
        } else {
            overrideLineRotation = false
        }

        if (Core.input.keyRelease(Binding.breakBlock) && Core.input.keyDown(Binding.schematicSelect) && mode == PlaceMode.breaking) {
            lastSchematic = Vars.schematics.create(schemX, schemY, rawCursorX, rawCursorY, tiles)
            schemX = -1
            schemY = -1
        }

        if (Core.input.keyRelease(Binding.breakBlock) || Core.input.keyRelease(Binding.select)) {
            if (mode == PlaceMode.placing && block != null) { //touch up while placing, place everything in selection
                if (Core.input.keyDown(Binding.boost)) {
                    flushPlansReverse(linePlans)
                } else {
                    flushPlans(linePlans)
                }

                linePlans.clear()
                Events.fire(LineConfirmEvent())
            } else if (mode == PlaceMode.breaking) { //touch up while breaking, break everything in selection
                removeSelection(
                    selectX,
                    selectY,
                    cursorX,
                    cursorY,
                    if (!Core.input.keyDown(Binding.schematicSelect)) InputHandler.maxLength else Vars.maxSchematicSize
                )
                if (lastSchematic != null) {
                    useSchematic(lastSchematic)
                    lastSchematic = null
                }
            }
            selectX = -1
            selectY = -1

            tryDropItems(
                if (selected == null) null else selected.build,
                Core.input.mouseWorld().x,
                Core.input.mouseWorld().y
            )

            val splan = splan
            if (splan != null) {
                if (getPlan(splan.x, splan.y, splan.tiles, splan.block.size, splan) != null) {
                    Vars.player.unit().plans().remove(splan, true)
                }

                if (Core.input.ctrl()) {
                    inv.hide()
                    config.hideConfig()
                    planConfig.showConfig(splan)
                } else {
                    planConfig.hide()
                }

                this.splan = null
            }

            mode = PlaceMode.none
        }


        //deselect if not placing
        if (!isPlacing() && mode == PlaceMode.placing) {
            mode = PlaceMode.none
        }

        if (Vars.player.shooting && !canShoot()) {
            Vars.player.shooting = false
        }

        if (isPlacing() && Vars.player.isBuilder()) {
            cursorType = SystemCursor.hand
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f)
        } else {
            selectScale = 0f
        }

        if (!Core.input.keyDown(Binding.diagonalPlacement) && abs(Core.input.axisTap(Binding.rotate).toInt()) > 0) {
            rotation = Mathf.mod(rotation + Core.input.axisTap(Binding.rotate).toInt(), 4)

            if (splan != null) {
                splan!!.rotation = Mathf.mod(splan!!.rotation + Core.input.axisTap(Binding.rotate).toInt(), 4)
            }

            if (isPlacing() && mode == PlaceMode.placing) {
                lineTiles = tiles
                updateLine(selectX, selectY)
            } else if (!selectPlans.isEmpty() && !Vars.ui.chatfrag.shown()) {
                rotatePlans(selectPlans, Mathf.sign(Core.input.axisTap(Binding.rotate)))
            }
        }

        val cursor = tileAt(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())

        cursorType = SystemCursor.arrow

        if (cursor != null) {
            if (cursor.build != null && cursor.build.interactable(Vars.player.team())) {
                cursorType = cursor.build.getCursor()
            }

            if (canRepairDerelict(cursor) && !Vars.player.dead() && Vars.player.unit().canBuild()) {
                cursorType = Vars.ui.repairCursor
            }

            if ((isPlacing() && Vars.player.isBuilder()) || !selectPlans.isEmpty()) {
                cursorType = SystemCursor.hand
            }

            if (!isPlacing() && canMine(cursor)) {
                cursorType = Vars.ui.drillCursor
            }

            if (commandMode && selectedUnits.any()) {
                var canAttack =
                    (cursor.build != null && !cursor.build.inFogTo(Vars.player.team()) && cursor.build.team !== Vars.player.team())

                if (!canAttack) {
                    val unit = selectedEnemyUnit(Core.input.mouseWorldX(), Core.input.mouseWorldY())
                    if (unit != null) {
                        canAttack = selectedUnits.contains { u -> u.canTarget(unit) }
                    }
                }

                if (canAttack) {
                    cursorType = Vars.ui.targetCursor
                }

                if (Core.input.keyTap(Binding.commandQueue) && Binding.commandQueue.value.key.type != KeyCode.KeyType.mouse) {
                    commandTap(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat(), true)
                }
            }

            if (getPlan(cursor.x.toInt(), cursor.y.toInt(), cursor.tiles) != null && mode == PlaceMode.none) {
                cursorType = SystemCursor.hand
            }

            if (canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)) {
                cursorType = Vars.ui.unloadCursor
            }

            if (cursor.build != null && cursor.interactable(Vars.player.team()) && !isPlacing() && abs(
                    Core.input.axisTap(
                        Binding.rotate
                    )
                ) > 0 && Core.input.keyDown(Binding.rotatePlaced) && cursor.block().rotate && cursor.block().quickRotate
            ) {
                Call.rotateBlock(Vars.player, cursor.build, Core.input.axisTap(Binding.rotate) > 0)
            }
        }
    }

    override fun tap(x: Float, y: Float, count: Int, button: KeyCode?): Boolean {
        if (Core.scene.hasMouse() || !commandMode) return false

        tappedOne = true

        //click: select a single unit
        if (button == KeyCode.mouseLeft) {
            if (count >= 2) {
                selectTypedUnits()
            } else {
                tapCommandUnit()
            }
        }

        return super.tap(x, y, count, button)
    }

    override fun touchDown(x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
        if (Core.scene.hasMouse() || !commandMode) return false

        if (button == KeyCode.mouseRight) {
            commandTap(x, y)
        }

        if (button == Binding.commandQueue.value.key) {
            commandTap(x, y, true)
        }

        return super.touchDown(x, y, pointer, button)
    }

    override fun selectedBlock(): Boolean {
        return isPlacing() && mode != PlaceMode.breaking
    }

    override fun getMouseX(): Float {
        return Core.input.mouseX().toFloat()
    }

    override fun getMouseY(): Float {
        return Core.input.mouseY().toFloat()
    }

    override fun updateState() {
        super.updateState()

        if (Vars.state.isMenu()) {
            lastSchematic = null
            droppingItem = false
            mode = PlaceMode.none
            block = null
            splan = null
            selectPlans.clear()
        }
    }

    override fun panCamera(position: Vec2?) {
        if (!locked()) {
            panning = true
            Core.camera.position.set(position)
        }
    }

    protected fun updateMovement(unit: Unit) {
        val omni = unit.type.omniMovement

        val speed = unit.speed()
        val xa = Core.input.axis(Binding.moveX)
        val ya = Core.input.axis(Binding.moveY)
        val boosted = (unit is Mechc && unit.isFlying())

        if (Core.settings.getBool("detach-camera")) {
            val targetPos = Core.camera.position

            movement.set(targetPos).sub(Vars.player).limit(speed)

            if (Vars.player.within(targetPos, 15f)) {
                movement.setZero()
                unit.vel.approachDelta(Vec2.ZERO, unit.speed() * unit.type().accel / 2f)
            }
        } else {
            movement.set(xa, ya).nor().scl(speed)
            if (Core.input.keyDown(Binding.mouseMove)) {
                movement.add(Core.input.mouseWorld().sub(Vars.player).scl(1f / 25f * speed)).limit(speed)
            }
        }

        val mouseAngle = Angles.mouseAngle(unit.x, unit.y)
        val aimCursor = omni && Vars.player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted

        if (aimCursor) {
            unit.lookAt(mouseAngle)
        } else {
            unit.lookAt(unit.prefRotation())
        }

        unit.movePref(movement)

        unit.aim(Core.input.mouseWorld())
        unit.controlWeapons(true, Vars.player.shooting && !boosted)

        Vars.player.boosting = Core.input.keyDown(Binding.boost)
        Vars.player.mouseX = unit.aimX()
        Vars.player.mouseY = unit.aimY()

        //update payload input
        if (unit is Payloadc) {
            if (Core.input.keyTap(Binding.pickupCargo)) {
                tryPickupPayload()
                lastPayloadKeyTapMillis = Time.millis()
            }

            if (Core.input.keyDown(Binding.pickupCargo)
                && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20 && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200
            ) {
                tryPickupPayload()
                lastPayloadKeyHoldMillis = Time.millis()
            }

            if (Core.input.keyTap(Binding.dropCargo)) {
                tryDropPayload()
                lastPayloadKeyTapMillis = Time.millis()
            }

            if (Core.input.keyDown(Binding.dropCargo)
                && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20 && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200
            ) {
                tryDropPayload()
                lastPayloadKeyHoldMillis = Time.millis()
            }
        }
    }
}
