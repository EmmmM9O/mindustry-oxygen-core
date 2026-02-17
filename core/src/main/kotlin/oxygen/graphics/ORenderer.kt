package oxygen.graphics

import arc.*
import arc.func.*
import arc.graphics.*
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.g2d.*
import arc.graphics.g3d.*
import arc.graphics.gl.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import mindustry.*
import mindustry.core.*
import mindustry.game.EventType.*
import mindustry.game.MapObjectives.MapObjective
import mindustry.world.blocks.environment.Floor.UpdateRenderState
import mindustry.gen.*
import mindustry.graphics.*
import mindustry.graphics.g3d.*
import mindustry.maps.*
import mindustry.world.*
import mindustry.type.*
import mindustry.entities.*
import mindustry.world.blocks.*
import oxygen.*
import oxygen.Oxygen.lightCam
import oxygen.Oxygen.lightDir
import oxygen.Oxygen.log
import oxygen.graphics.bloom.*
import oxygen.math.*
import oxygen.world.*
import oxygen.world.blocks.*
import oxygen.graphics.OBlockRenderer.*
import kotlin.math.*

class ORenderer : RendererI() {
    val customBackgrounds: ObjectMap<String, Runnable> = ObjectMap()
    val envRenderers: Seq<EnvRenderer> = Seq.with()

    private val clearColor = Color(0f, 0f, 0f, 1f)

    //for landTime > 0: if true, core is currently *launching*, otherwise landing.
    private val camShakeOffset = Vec2()
    private var glErrors = 0
    var blocksL = OBlockRenderer()

    //g3d
    var dis = 150f
    val cam = Camera3D()

    val camPos = Vec3(0f, 0f, 20f)

    val tmpMat = Mat3D()
    val tmpMat1 = Mat3D()
    val tmpMat2 = Mat3D()

    // Shaderi
    val shadowMapSize = 1024
    val shadowBuffer = FrameBuffer(shadowMapSize, shadowMapSize, true)

    //Test
    var showShadowMap = false
    var lightFar = 100f
    var lightDir_ = lightDir

    lateinit var obloom: OBloom

    //Tiles
    val renderDatas: Seq<TilesRenderData> = Seq.with()

    fun getData(tiles: Tiles): TilesRenderData {
        while (renderDatas.size <= tiles.id) {
            renderDatas.add(null as TilesRenderData?)
        }
        return (renderDatas.get(tiles.id) ?: createData(tiles)).also {
            if (it.tiles != tiles) {
                it.tiles = tiles
                reloadData(it)
            }
        }
    }

    fun reloadData(data: TilesRenderData) {
        renderDatas.set(data.tiles.id, data)
        blocksL.floorL.reload(data, false)
        blocksL.reload(data)
    }

    fun createData(tiles: Tiles): TilesRenderData =
        TilesRenderData(tiles).also(::reloadData)

    fun reload() {
        for (data in renderDatas) {
            if (data == null) continue
            data.dispose()
        }
        renderDatas.clear()
        getData(Vars.world.tiles)
    }

    init {
        blocks = blocksL
        Core.camera = Camera()
        Shaders.init()

        Events.on(ResetEvent::class.java) {
            shakeReduction = 0f
            shakeIntensity = shakeReduction
            shakeTime = shakeIntensity
            camShakeOffset.setZero()
        }

        drawUnitShaodw = false

        cam.fov = 90f
        cam.far = 120f
        cam.up.set(Vec3.Y)

        lightCam.perspective = false
        lightCam.fov = 90f
        lightCam.far = 200f
        lightCam.up.set(Vec3.Y)
    }

    override fun addEnvRenderer(mask: Int, render: Runnable) {
        envRenderers.add(EnvRenderer(mask, render))
    }

    override fun addCustomBackground(name: String, render: Runnable) {
        customBackgrounds.put(name, render)
    }

    override fun init() {
        OCShaders.init()
        obloom = StandardDualFilterBloom()
        /*
        obloom = CompareBloom().apply {
            blooms.add(PyramidStandardDualBloom())
            blooms.add(PyramidFourNAvgBloom())
            blooms.add(StandardDualFilterBloom())
        }*/
        planets = PlanetRenderer()

        if (Core.settings.getBool("bloom", true)) {
            setupBloom()
        }

        EnvRenderers.init()
        for (i in bubbles.indices) bubbles[i] = Core.atlas.find("bubble-$i")
        for (i in splashes.indices) splashes[i] = Core.atlas.find("splash-$i")

        loadFluidFrames()

        Events.on(ClientLoadEvent::class.java) {
            loadFluidFrames()
        }

        Core.assets.load("sprites/clouds.png", Texture::class.java).loaded = Cons { t: Texture? ->
            t!!.setWrap(TextureWrap.repeat)
            t.setFilter(TextureFilter.linear)
        }

        Events.on(WorldLoadEvent::class.java) {
            //reset background buffer on every world load, so it can be re-cached first render
            if (backgroundBuffer != null) {
                backgroundBuffer.dispose()
                backgroundBuffer = null
            }
        }

        Events.run(Trigger.tilesInit) {
            reload()
            blocksL.floor.reload()
            if (blocksL.hadMapLimit && !Vars.state.rules.limitMapArea) {
                blocks.updateDarkness()
                Vars.renderer.minimap.updateAll()
            }
        }

        EffectConfigs.init()
        EffectConfigs.default()
    }

    override fun loadFluidFrames() {
        fluidFrames = Array<Array<TextureRegion?>?>(2) { arrayOfNulls(Liquid.animationFrames) }

        val fluidTypes = arrayOf<String?>("liquid", "gas")

        for (i in fluidTypes.indices) {
            for (j in 0..<Liquid.animationFrames) {
                fluidFrames[i][j] = Core.atlas.find("fluid-" + fluidTypes[i] + "-" + j)
            }
        }
    }

    override fun getFluidFrames(): Array<Array<TextureRegion?>?>? {
        if (fluidFrames == null || fluidFrames[0][0].texture.isDisposed) {
            loadFluidFrames()
        }
        return fluidFrames
    }

    override fun update() {
        PerfCounter.render.begin()
        Color.white.set(1f, 1f, 1f, 1f)

        var baseTarget = targetscale

        if (Vars.control.input.logicCutscene) {
            baseTarget = Mathf.lerp(minZoom, maxZoom, Vars.control.input.logicCutsceneZoom)
        }

        val dest = Mathf.clamp(Mathf.round(baseTarget, 0.5f), minScale(), maxScale())
        camerascale = Mathf.lerpDelta(camerascale, dest, 0.1f)
        if (Mathf.equal(camerascale, dest, 0.001f)) camerascale = dest
        Renderer.unitLaserOpacity = Core.settings.getInt("unitlaseropacity") / 100f
        Renderer.laserOpacity = Core.settings.getInt("lasersopacity") / 100f
        Renderer.bridgeOpacity = Core.settings.getInt("bridgeopacity") / 100f
        //TODO
        //animateShields = Core.settings.getBool("animatedshields")
        //animateWater = Core.settings.getBool("animatedwater")
        drawStatus = Core.settings.getBool("blockstatus")
        enableEffects = Core.settings.getBool("effects")
        drawDisplays = !Core.settings.getBool("hidedisplays")
        maxZoomInGame = Core.settings.getFloat("maxzoomingamemultiplier", 1f) * maxZoom
        minZoomInGame = minZoom / Core.settings.getFloat("minzoomingamemultiplier", 1f)
        drawLight = Core.settings.getBool("drawlight", true)
        pixelate = Core.settings.getBool("pixelate")

        //don't bother drawing landing animation if core is null
        /*
        if (launchAnimator == null) landTime = 0f
        if (landTime > 0) {
            if (!Vars.state.isPaused) launchAnimator.updateLaunch()

            weatherAlpha = 0f
            camerascale = launchAnimator.zoomLaunch()

            if (!Vars.state.isPaused) landTime -= Time.delta
        } else {
            weatherAlpha = Mathf.lerpDelta(weatherAlpha, 1f, 0.08f)
        }

        if (launchAnimator != null && landTime <= 0f) {
            launchAnimator.endLaunch()
            launchAnimator = null
        }*/

        if (Vars.state.isMenu) {
            landTime = 0f
            Core.graphics.clear(Color.black)
        } else {
            minimap.update()

            if (shakeTime > 0) {
                val intensity = shakeIntensity * (Core.settings.getInt("screenshake", 4) / 4f) * 0.75f
                camShakeOffset.setToRandomDirection().scl(Mathf.random(intensity))
                Core.camera.position.add(camShakeOffset)
                shakeIntensity -= shakeReduction * Time.delta
                shakeTime -= Time.delta
                shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f)
            } else {
                camShakeOffset.setZero()
                shakeIntensity = 0f
            }

            if (Vars.renderer.pixelate) {
                pixelator.drawPixelate()
                pixelator.begin()
            } else {
                draw()
            }

            Core.camera.position.sub(camShakeOffset)
        }

        //glGetError can be expensive, so only check it periodically
        if (glErrors < Vars.maxGlErrors && Core.graphics.frameId % 10 == 0L) {
            val error = Gl.getError()
            if (error != Gl.noError) {
                val mes = when (error) {
                    Gl.invalidValue -> "invalid value"
                    Gl.invalidOperation -> "invalid operation"
                    Gl.invalidFramebufferOperation -> "invalid framebuffer operation"
                    Gl.invalidEnum -> "invalid enum"
                    Gl.outOfMemory -> "out of memory"
                    else -> "unknown error ($error)"
                }

                log.atInfo {
                    mark(Marks.gl)
                    message(mes)
                }
                glErrors++
            }
        }

        PerfCounter.render.end()
    }

    override fun updateAllDarkness() {}
    override fun dispose() {
        Events.fire(DisposeEvent())
    }

    override fun resume() {
        if (Core.settings.getBool("bloom") && bloom != null) {
            bloom.resume()
        }
    }

    fun setupBloom() {
        try {
            if (bloom != null) {
                bloom.dispose()
                bloom = null
            }
            bloom = Bloom(true)
        } catch (e: Throwable) {
            Core.settings.put("bloom", false)
            Vars.ui.showErrorMessage("@error.bloom")
            Log.err(e)
        }
    }

    override fun toggleBloom(enabled: Boolean) {
        if (enabled) {
            if (bloom == null) {
                setupBloom()
            }
        } else {
            if (bloom != null) {
                bloom.dispose()
                bloom = null
            }
        }
    }

    fun drawGroups() {
        Groups.draw.draw(::drawObj)
    }

    fun drawObj(obj: Drawc) {
        if (obj is Heightc) {
            OGraphics.realZ(obj.height())
            ZDraw.height = obj.height() + 4f
        } else {
            OGraphics.realZ(4f)
            ZDraw.height = 4f
        }
        if (obj is TilesCraftc) {
            if (obj.craftType().drawUnit) obj.draw()
        } else obj.draw()
        ZDraw.height = 0f
    }

    fun drawFloor(depth: Boolean) {
        OGraphics.realZ(0f)
        //Draw.draw(Layer.background) { this.drawBackground() }
        Draw.draw(Layer.floor) {
            blocksL.floorL.drawDepth = depth
            blocksL.floor.drawFloor()
        }
        /*
        Draw.drawRange(
            Layer.blockBuilding,
            { Draw.shader(Shaders.blockbuild, true) },
            { Draw.shader() })
            */
        //render all matching environments
    }

    fun updateLight(width: Float, height: Float) {
        val dir = lightDir
        lightCam.position.set(dir).scl(-lightFar)
        if (abs(dir.z) > 0.95f) {
            lightCam.resize(width, height)
        } else {
            val normal = Vec3.Z
            val cosAngle = abs(dir.dot(normal))
            if (cosAngle > 0.001f) {
                val scaleFactor = 1.0f / cosAngle;
                lightCam.resize(width * scaleFactor, height * scaleFactor)
            } else {

            }
        }
        lightCam.lookAt(Vec3.Zero)
        lightCam.update()
    }

    var sclColor = Color(0.12f, 0.12f, 0.12f, 0.1f)

    val objs: Seq<Object> = Seq.with()

    override fun draw() {
        val w = Core.graphics.width.toFloat()
        val h = Core.graphics.height.toFloat()
        camPos.set(0f, 0f, dis / camerascale)

        Events.fire(Trigger.preDraw)

        if (java.lang.Float.isNaN(Core.camera.position.x) || java.lang.Float.isNaN(Core.camera.position.y)) {
            Core.camera.position.set(Vars.player)
        }
        //
        val tw = dis / camerascale //* 2f
        val aspect = w / h
        val cw = tw * aspect
        val ch = tw
        Core.camera.width = cw * 8f
        Core.camera.height = ch * 8f
        Core.camera.update()

        /*if (animateWater || animateShields) {
            effectBuffer.resize(Core.graphics.width, Core.graphics.height)
        }*/
        MapPreviewLoader.checkPreviews()

        blocksL.checkChanges()
        blocksL.floor.checkChanges()
        blocksL.processBlocks()
        blocksL.processShadows()
        blocksL.processDarkness()

        tmpMat1.set(OGraphics.proj3D())
        tmpMat2.set(OGraphics.trans3D())
        tmpMat.setToScaling(cw, tw, 1f / 8f)
        OGraphics.trans3D(tmpMat)
        Draw.proj(Core.camera)

        updateLight(cw * 2f, ch * 2f)

        OGraphics.proj3D(lightCam.combined)
        Oxygen.trans3D.set(OGraphics.trans3D()).mul(Core.camera.mat.to3D(tmpMat))
        OGraphics.drawDepth(true)

        OGraphics.zbatch.alphaTest = 0.95f

        Draw.sort(true)
        Gl.depthMask(true)

        if (!showShadowMap) shadowBuffer.begin()
        DepthFunc.lequal.apply()
        Core.graphics.clear(Color.white)
        Gl.clear(Gl.depthBufferBit or Gl.colorBufferBit)

        Gl.enable(Gl.depthTest)
        Gl.enable(Gl.cullFace)
        Gl.cullFace(Gl.front)
        blocksL.draw3DDepth()
        Gl.disable(Gl.cullFace)

        drawFloor(true)
        drawGroups()
        Draw.flush()
        Draw.reset()
        OGraphics.zbatch.alphaTest = 0f
        if (!showShadowMap) {
            shadowBuffer.end()
            OGraphics.drawDepth(false)

            val shader = OGShaders.zbatchShadow
            shader.lightMat = lightCam.combined
            shader.shadowMap = shadowBuffer.texture
            shader.lightDir = lightDir

            cam.position.set(camPos)
            cam.lookAt(Vec3.Zero)
            cam.resize(Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
            cam.update()

            OGraphics.proj3D(cam.combined)

            Events.fire(Trigger.draw)
            /*
            if (Vars.renderer.pixelate) {
                pixelator.register()
            }*/

            val holder = object {
                var color = 0f
            }

            OGraphics.zbatch.beforeVert = func1@{
                val z = Draw.z()
                if (z < Layer.bullet - 0.02f || z > Layer.effect + 0.02f) return@func1
                holder.color = sclColorPacked
                sclColorPacked = sclColor.toFloatBits()
            }
            OGraphics.zbatch.afterVert = func2@{
                val z = Draw.z()
                if (z < Layer.bullet - 0.02f || z > Layer.effect + 0.02f) return@func2
                sclColorPacked = holder.color
            }
            obloom.resize(Core.graphics.width, Core.graphics.height)
            obloom.capture()
            DepthFunc.lequal.apply()
            Core.graphics.clear(clearColor)
            Gl.clear(Gl.depthBufferBit)

            Draw.sort(false)
            Draw.shader(shader)
            Draw.sort(true)

            objs.clear()
            for (tiles in Vars.world.allTiles) {
                if (tiles.craft == null) continue
                objs.add(tiles.craft as Object)
            }
            Groups.draw.draw { obj: Drawc ->
                objs.add(obj as Object)
            }
            objs.sort { obj -> if (obj is Heightc) obj.height else 0f }

            blocksL.floorL.drawDepth = false

            for (obj in objs) {
                if (obj is TilesCraftc) {
                    ZDraw.height = obj.height() + 4f
                    if (obj.craftType().drawUnit) obj.draw()
                    val tiles = obj.tiles()
                    val data = getData(tiles)

                    Gl.enable(Gl.cullFace)
                    Gl.cullFace(Gl.back)
                    blocksL.g3dEach(data, G3DrawBuilding::draw3D)
                    Gl.disable(Gl.cullFace)

                    blocksL.floorL.realZ = 0f
                    blocksL.floorL.drawFloor(data)
                    Draw.flush()

                    OGraphics.realZ(0f)
                    Draw.draw(Layer.block - 1f) {
                        blocksL.drawShadows(data)
                    }
                    Draw.flush()

                    OGraphics.realZ(0f)
                    Draw.z(Layer.block - 0.09f)
                    blocksL.floorL.realZ = 4f
                    blocksL.floorL.beginDraw(data)
                    blocksL.floorL.drawLayer(data, CacheLayer.walls)
                    blocksL.floorL.realZ = 0f
                    Draw.flush()

                    Draw.draw(Layer.min) {
                        Draw.shader(shader)
                    }
                    OGraphics.realZ(0f)
                    Draw.z(Layer.block)
                    blocksL.drawBlocks(data)
                    ZDraw.height = 0f
                } else if (obj is Drawc) {
                    drawObj(obj)
                }
            }

            Draw.sort(false)
            Draw.shader()
            Draw.sort(true)
            Draw.reset()
            /*
            if (bloom != null) {
                bloom.resize(Core.graphics.width, Core.graphics.height)
                bloom.setBloomIntensity(Core.settings.getInt("bloomintensity", 6) / 4f + 1f)
                bloom.blurPasses = Core.settings.getInt("bloomblur", 1)
                Draw.draw(Layer.bullet - 0.02f) { bloom.capture() }
                Draw.draw(Layer.effect + 0.02f) { bloom.render() }
            }*/


            /* TODO animateShields support
            if (animateShields && Shaders.shield != null) {
                //TODO would be nice if there were a way to detect if any shields or build beams actually *exist* before beginning/ending buffers, otherwise you're just blitting and swapping shaders for nothing
                Draw.drawRange(Layer.shields, 1f, { effectBuffer.begin(Color.clear) }, {
                    effectBuffer.end()
                    effectBuffer.blit(Shaders.shield)
                })

                Draw.drawRange(Layer.buildBeam, 1f, { effectBuffer.begin(Color.clear) }, {
                    effectBuffer.end()
                    effectBuffer.blit(Shaders.buildBeam)
                })
            }
                Draw.draw(Layer.space) {
                    if (launchAnimator != null && landTime > 0f) launchAnimator.drawLaunch()
                }
                if (launchAnimator != null) {
                    Draw.z(Layer.space)
                    launchAnimator.drawLaunchGlobalZ()
                    Draw.reset()
                }*/

            if (Vars.drawDebugHitboxes) {
                DebugCollisionRenderer.draw()
            }

            OGraphics.realZ(0f)
            Draw.flush()

            obloom.render()

            OGraphics.proj3D(tmpMat1)
            OGraphics.trans3D(tmpMat2)

            Gl.disable(Gl.depthTest)

            Events.fire(Trigger.drawOver)

            OGraphics.realZ(2.0f)
            val scaleFactor = 4f / Vars.renderer.displayScale

            //draw objective markers

            for (renderer in envRenderers) {
                if ((renderer.env and Vars.state.rules.env) == renderer.env) {
                    renderer.renderer.run()
                }
            }
            OGraphics.realZ(0.2f)
            if (Vars.state.rules.fog) Draw.draw(Layer.fogOfWar) { fog.drawFog() }
            if (Vars.state.rules.lighting && drawLight) {
                Draw.draw(Layer.light) { lights.draw() }
            }
            if (Vars.enableDarkness) {
                Draw.draw(Layer.darkness) { blocksL.drawDarkness() }
            }
            Draw.flush()

            Vars.state.rules.objectives.eachRunning { obj: MapObjective? ->
                for (marker in obj!!.markers) {
                    if (marker.world) {
                        marker.draw(if (marker.autoscale) scaleFactor else 1f)
                    }
                }
            }

            for (marker in Vars.state.markers) {
                if (marker.world) {
                    marker.draw(if (marker.autoscale) scaleFactor else 1f)
                }
            }
            Vars.control.input.drawCommanded()
            Draw.draw(Layer.plans) { overlays.drawBottom() }
            Draw.draw(Layer.overlayUI) {
                overlays.drawTop()
            }
            Draw.flush()

            OGraphics.zbatch.beforeVert = null
            OGraphics.zbatch.afterVert = null
        }
        OGraphics.drawDepth(false)

        Draw.sort(false)
        Draw.shader()
        Gl.depthMask(false)
        Gl.disable(Gl.depthTest)
        OGraphics.proj3D(tmpMat1)
        OGraphics.trans3D(tmpMat2)

        Events.fire(Trigger.postDraw)
    }

    override fun showLanding(landCore: LaunchAnimator) {}
    override fun showLaunch(landCore: LaunchAnimator) {}
    override fun takeMapScreenshot() {
        val w = Vars.world.width() * Vars.tilesize
        val h = Vars.world.height() * Vars.tilesize
        val memory = w * h * 4 / 1024 / 1024

        if (Vars.checkScreenshotMemory && memory >= (if (Vars.mobile) 65 else 120)) {
            Vars.ui.showInfo("@screenshot.invalid")
            return
        }

        val buffer = FrameBuffer(w, h)

        drawWeather = false
        val vpW = Core.camera.width
        val vpH = Core.camera.height
        val px = Core.camera.position.x
        val py = Core.camera.position.y
        Vars.disableUI = true
        Core.camera.width = w.toFloat()
        Core.camera.height = h.toFloat()
        Core.camera.position.x = w / 2f + Vars.tilesize / 2f
        Core.camera.position.y = h / 2f + Vars.tilesize / 2f
        buffer.begin()
        draw()
        Draw.flush()
        val lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true)
        buffer.end()
        Vars.disableUI = false
        Core.camera.width = vpW
        Core.camera.height = vpH
        Core.camera.position.set(px, py)
        drawWeather = true
        buffer.dispose()

        Threads.thread {
            var i = 0
            while (i < lines.size) {
                lines[i + 3] = 255.toByte()
                i += 4
            }
            val fullPixmap = Pixmap(w, h)
            Buffers.copy(lines, 0, fullPixmap.pixels, lines.size)
            val file = Vars.screenshotDirectory.child("screenshot-" + Time.millis() + ".png")
            PixmapIO.writePng(file, fullPixmap)
            fullPixmap.dispose()
            Core.app.post { Vars.ui.showInfoFade(Core.bundle.format("screenshot", file.toString())) }
        }
    }

    class EnvRenderer(val env: Int, val renderer: Runnable)

    companion object {
        val initialRequests: Int = 32 * 32
    }
}

open class TilesRenderData(var tiles: Tiles) : Disposable {
    var cache: Array<Array<Array<ChunkMesh?>?>?>? = null
    val recacheSet = IntSet()
    val used = ObjectSet<CacheLayer>()

    val drawnLayerSet = IntSet()
    val drawnLayers = IntSeq()

    var packWidth = 0f
    var packHeight = 0f

    val tileview = Seq<Tile>(false, ORenderer.initialRequests, Tile::class.java)
    val g3dview = Seq<Tile>(false, ORenderer.initialRequests, Tile::class.java)
    val lightview = Seq<Tile>(false, ORenderer.initialRequests, Tile::class.java)

    val procLinks = IntSet()
    val procLights = IntSet()
    val proc3D = IntSet()

    var blockTree = BlockQuadtree(Rect(0f, 0f, 1f, 1f))
    var blockLightTree = BlockLightQuadtree(Rect(0f, 0f, 1f, 1f))
    var block3DTree = Block3DQuadtree(Rect(0f, 0f, 1f, 1f))
    var floorTree = FloorQuadtree(Rect(0f, 0f, 1f, 1f))

    val updateFloors = Seq<UpdateRenderState>(UpdateRenderState::class.java)

    val shadows = FrameBuffer()
    val dark = FrameBuffer()
    val outArray2 = Seq<Building>()
    val shadowEvents = Seq<Tile>()
    val darkEvents = IntSet()

    override fun dispose() {
        if (cache != null) {
            for (x in cache) {
                for (y in x!!) {
                    for (mesh in y!!) {
                        mesh?.dispose()
                    }
                }
            }
        }
        shadows.dispose()
        dark.dispose()
    }
}
