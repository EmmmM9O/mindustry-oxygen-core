package oxygen.graphics

import arc.*
import arc.func.*
import arc.graphics.*
import arc.graphics.g2d.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.ai.types.*
import mindustry.entities.*
import mindustry.game.*
import mindustry.game.EventType.CoreChangeEvent
import mindustry.game.EventType.WorldLoadEvent
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.graphics.*
import mindustry.input.*
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import oxygen.*
import kotlin.math.*

class OOverlayRenderer : OverlayRendererI() {
    private var buildFade = 0f
    private var unitFade = 0f
    private var lastSelect: Sized? = null
    private val cedges = Seq<CoreEdge>()
    private var updatedCores = false

    init {
        Events.on(WorldLoadEvent::class.java) { e ->
            updatedCores = true
        }

        Events.on(CoreChangeEvent::class.java) { e ->
            updatedCores = true
        }
    }

    private fun updateCoreEdges() {
        if (!updatedCores) {
            return
        }

        updatedCores = false
        cedges.clear()

        val pos = Seq<Vec2>()
        val teams = Seq<CoreBuild>()
        for (team in Vars.state.teams.active) {
            for (b in team.cores) {
                teams.add(b)
                pos.add(Vec2(b.x, b.y))
            }
        }

        if (pos.isEmpty()) {
            return
        }

        //if this is laggy, it could be shoved in another thread.
        val result = Voronoi.generate(
            pos.toArray<Vec2>(Vec2::class.java),
            0f,
            Vars.world.unitWidth().toFloat(),
            0f,
            Vars.world.unitHeight().toFloat()
        )
        for (edge in result) {
            cedges.add(
                CoreEdge(
                    edge.x1,
                    edge.y1,
                    edge.x2,
                    edge.y2,
                    teams.get(edge.site1).team,
                    teams.get(edge.site2).team
                )
            )
        }
    }

    override fun drawBottom() {
        val input = Vars.control.input

        if (Vars.player.dead()) return

        if (Vars.player.isBuilder()) {
            Vars.player.unit().drawBuildPlans()
        }

        input.drawBottom()
    }

    override fun drawTop() {
        if (!Vars.player.dead() && Vars.ui.hudfrag.shown) {
            if (Core.settings.getBool("playerindicators")) {
                for (player in Groups.player) {
                    if (Vars.player !== player && Vars.player.team() === player.team()) {
                        if (!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                                .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)
                        ) {
                            Tmp.v1.set(player).sub(Vars.player).setLength(indicatorLength)

                            Lines.stroke(2f, Vars.player.team().color)
                            Lines.lineAngle(Vars.player.x + Tmp.v1.x, Vars.player.y + Tmp.v1.y, Tmp.v1.angle(), 4f)
                            Draw.reset()
                        }
                    }
                }
            }

            if (Core.settings.getBool("indicators") && !Vars.state.rules.fog) {
                Groups.unit.each(Cons { unit ->
                    if (!unit.isLocal() && unit.team !== Vars.player.team() && !rect.setSize(
                            Core.camera.width * 0.9f, Core.camera.height * 0.9f
                        )
                            .setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x, unit.y)
                    ) {
                        Tmp.v1.set(unit.x, unit.y).sub(Vars.player).setLength(indicatorLength)

                        Lines.stroke(1f, unit.team().color)
                        Lines.lineAngle(Vars.player.x + Tmp.v1.x, Vars.player.y + Tmp.v1.y, Tmp.v1.angle(), 3f)
                        Draw.reset()
                    }
                })
            }
        }

        val input = Vars.control.input

        var select: Sized? = input.selectedUnit()
        if (select == null) select = input.selectedControlBuild()
        if (!Core.input.keyDown(Binding.control) || !Vars.state.rules.possessionAllowed) select = null

        unitFade = Mathf.lerpDelta(unitFade, Mathf.num(select != null).toFloat(), 0.1f)

        if (select != null) lastSelect = select
        if (select == null) select = lastSelect
        if (select != null && (select !is Unitc || select.isAI())) {
            Draw.mixcol(Pal.accent, 1f)
            Draw.alpha(unitFade)
            val build = (if (select is BlockUnitc) select.tile() else if (select is Building) select else null)
            val region = if (build != null) build.block.fullIcon else Core.atlas.white()

            if (select is BlockUnitc) {
                Draw.rect(region, select.getX(), select.getY())
            }

            for (i in 0..3) {
                val rot = i * 90f + 45f + (-Time.time) % 360f
                val length = select.hitSize() * 1.5f + (unitFade * 2.5f)
                Draw.rect(
                    "select-arrow",
                    select.getX() + Angles.trnsx(rot, length),
                    select.getY() + Angles.trnsy(rot, length),
                    length / 1.9f,
                    length / 1.9f,
                    rot - 135f
                )
            }

            Draw.reset()
        }

        if (!Vars.player.dead()) input.drawTop()
        input.drawUnitSelection()

        if (Vars.player.dead()) return  //dead players don't draw


        buildFade = Mathf.lerpDelta(buildFade, if (input.isPlacing() || input.isUsingSchematic()) 1f else 0f, 0.06f)

        Draw.reset()
        Lines.stroke(buildFade * 2f)

        if (buildFade > 0.005f) {
            if (Vars.state.rules.polygonCoreProtection) {
                updateCoreEdges()
                Draw.color(Pal.accent)

                for (i in 0..1) {
                    val offset = (if (i == 0) -2f else 0f)
                    for (edge in cedges) {
                        val displayed = edge.displayed()
                        if (displayed != null) {
                            Draw.color(
                                if (i == 0) Color.darkGray else Tmp.c1.set(displayed.color).lerp(
                                    Pal.accent, Mathf.absin(
                                        Time.time, 10f, 0.2f
                                    )
                                )
                            )
                            Lines.line(edge.x1, edge.y1 + offset, edge.x2, edge.y2 + offset)
                        }
                    }
                }

                Draw.color()
            } else {
                Vars.state.teams.eachEnemyCore(Vars.player.team(), Cons { core: Building? ->
                    //it must be clear that there is a core here.
                    val br = Vars.state.rules.buildRadius(core!!.team)
                    if ( /*core.wasVisible && */Core.camera.bounds(Tmp.r1)
                            .overlaps(Tmp.r2.setCentered(core.x, core.y, br * 2f))
                    ) {
                        Draw.color(Color.darkGray)
                        Lines.circle(core.x, core.y - 2, br)
                        Draw.color(Pal.accent, core.team.color, 0.5f + Mathf.absin(Time.time, 10f, 0.5f))
                        Lines.circle(core.x, core.y, br)
                    }
                })
            }
        }

        Lines.stroke(2f)
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f))

        if (Vars.state.hasSpawns()) {
            for (tile in Vars.spawner.getSpawns()) {
                if (tile.within(Vars.player.x, Vars.player.y, Vars.state.rules.dropZoneRadius + spawnerMargin)) {
                    Draw.alpha(Mathf.clamp(1f - (Vars.player.dst(tile) - Vars.state.rules.dropZoneRadius) / spawnerMargin))
                    Lines.dashCircle(tile.worldx(), tile.worldy(), Vars.state.rules.dropZoneRadius)
                }
            }
        }

        Draw.reset()

        val hover = Vars.ui.hudfrag.blockfrag.hover()
        if (hover is Unit) {
            val ai = hover.controller()
            if (ai is LogicAI && ai.controller != null && ai.controller.isValid() && (Vars.state.isEditor() || !ai.controller.block.privileged)) {
                val build: Building = ai.controller
                Drawf.square(build.absoluteX, build.absoluteY, build.block.size * Vars.tilesize / 2f + 2f)
                if (!hover.within(build, hover.hitSize * 2f)) {
                    Drawf.arrow(hover.x, hover.y, build.absoluteX, build.absoluteY, hover.hitSize * 2f, 4f)
                }
            }
        }

        //draw selection overlay when dropping item
        if (input.isDroppingItem()) {
            val v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY())
            val size = 8f
            Draw.rect(Vars.player.unit().item().fullIcon, v.x, v.y, size, size)
            Draw.color(Pal.accent)
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time, 5f, 1f))
            Draw.reset()

            val build = Vars.world.buildWorld(v.x, v.y)
            if (input.canDropItem() && build != null && build.interactable(Vars.player.team()) && build.acceptStack(
                    Vars.player.unit().item(), Vars.player.unit().stack.amount, Vars.player.unit()
                ) > 0 && Vars.player.within(build, Vars.itemTransferRange) && input.itemDepositCooldown <= 0f
            ) {
                val invalid = !build.allowDeposit()

                Lines.stroke(3f, Pal.gray)
                Lines.square(
                    build.x,
                    build.y,
                    build.block.size * Vars.tilesize / 2f + 3 + Mathf.absin(Time.time, 5f, 1f)
                )
                Lines.stroke(1f, if (invalid) Pal.remove else Pal.place)
                Lines.square(
                    build.x,
                    build.y,
                    build.block.size * Vars.tilesize / 2f + 2 + Mathf.absin(Time.time, 5f, 1f)
                )
                Draw.reset()

                if (invalid) {
                    build.block.drawPlaceText(
                        Core.bundle.get("bar.onlycoredeposit"),
                        build.tileX(),
                        build.tileY(),
                        false
                    )
                }
            }
        }

        //Flush
        //draw config selected block
        if (input.config.isShown()) {
            val tile = input.config.getSelected()
            Oxygen.renderer.drawTilesFunc(tile.tiles.craft) {
                tile.drawConfigure()
            }
        }

        //draw selected block
        if (input.block == null && !Core.scene.hasMouse()) {
            val tile = Oxygen.input.tileAtF(input.getMouseX(), input.getMouseY())

            if (tile?.build != null && tile?.build!!.team === Vars.player.team()) {
                val build = tile.build
                Oxygen.renderer.drawTilesFunc(tile.tiles.craft) {
                    build.drawSelect()
                    if (!build.enabled && build.block.drawDisabled) {
                        build.drawDisabled()
                    }

                    if (Core.input.keyDown(Binding.rotatePlaced) && build.block.rotate && build.block.quickRotate && build.interactable(
                            Vars.player.team()
                        )
                    ) {
                        Vars.control.input.drawArrow(build.block, build.tileX(), build.tileY(), build.rotation, true)
                        Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f))
                        Fill.square(build.x, build.y, build.block.size * Vars.tilesize / 2f)
                        Draw.color()
                    }
                }
            }
        }

        input.drawOverSelect()
    }

    override fun checkApplySelection(u: Unit?) {
        if (unitFade > 0.001f && lastSelect === u) {
            val prev = Draw.getMixColor()
            Draw.mixcol(if (prev.a > 0.001f) prev.lerp(Pal.accent, unitFade) else Pal.accent, max(unitFade, prev.a))
        }
    }

    private class CoreEdge(var x1: Float, var y1: Float, var x2: Float, var y2: Float, var t1: Team, var t2: Team) {
        @Nullable
        fun displayed(): Team? {
            return if (t1 === t2) null else if (t1 === Vars.player.team()) t2 else if (t2 === Vars.player.team()) t1 else if (t2.id == 0) t1 else if (t1.id < t2.id && t1.id != 0) t1 else t2
        }
    }

    companion object {
        private const val indicatorLength = 14f
        private val spawnerMargin = Vars.tilesize * 11f
        private val rect = Rect()
    }
}
