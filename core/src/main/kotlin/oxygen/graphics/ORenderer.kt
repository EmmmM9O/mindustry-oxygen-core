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
import mindustry.gen.*
import mindustry.graphics.*
import mindustry.graphics.g3d.*
import mindustry.maps.*
import mindustry.type.*
import mindustry.world.blocks.*
import oxygen.*
import oxygen.Oxygen.log

class ORenderer : RendererI() {
    val customBackgrounds: ObjectMap<String, Runnable> = ObjectMap()

    private val clearColor = Color(0f, 0f, 0f, 1f)

    //for landTime > 0: if true, core is currently *launching*, otherwise landing.
    private val camShakeOffset = Vec2()
    private var glErrors = 0
    var blocksL = OriBlockRenderer()

    //g3d
    var dis = 80f
    var offset = -1f
    val cam = Camera3D()
    val tmpTilesBuffer = FrameBuffer()
    val camPos = Vec3(0f, 20f, 0f)

    val tilesMesh = Meshes.texturePlane(1f, 1f)
    val tmpMat = Mat3D()

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

        cam.fov = 90f
        cam.far = 400f
        cam.up.set(Vec3.Z)
    }

    override fun addEnvRenderer(mask: Int, render: Runnable) {}
    override fun addCustomBackground(name: String, render: Runnable) {
        customBackgrounds.put(name, render)
    }

    override fun init() {
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
        animateShields = Core.settings.getBool("animatedshields")
        animateWater = Core.settings.getBool("animatedwater")
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

    override fun draw() {
        val w = Core.graphics.width.toFloat()
        val h = Core.graphics.height.toFloat()
        camPos.set(0f, dis / camerascale, 0f)
        tmpTilesBuffer.resize(Core.graphics.width, Core.graphics.height)
        tmpTilesBuffer.begin(Color.white)
        Core.graphics.clear(Color.white)
        //Draw.flush()
        Events.fire(Trigger.preDraw)

        if (java.lang.Float.isNaN(Core.camera.position.x) || java.lang.Float.isNaN(Core.camera.position.y)) {
            Core.camera.position.set(Vars.player)
        }
        //
        val tw = dis / camerascale * 2f
        val aspect = w / h
        Core.camera.width = tw * 8f * aspect
        Core.camera.height = tw * 8f
        Core.camera.update()

        if (animateWater || animateShields) {
            effectBuffer.resize(Core.graphics.width, Core.graphics.height)
        }

        Draw.proj(Core.camera)

        blocksL.checkChanges()
        blocksL.floor.checkChanges()
        blocksL.processBlocks()

        Draw.sort(true)
        Events.fire(Trigger.draw)
        MapPreviewLoader.checkPreviews()

        if (Vars.renderer.pixelate) {
            pixelator.register()
        }
        Draw.draw(Layer.end) {
            tmpTilesBuffer.end()
            Blending.disabled.apply()
            Core.graphics.clear(clearColor)

            //3D
            Gl.clear(Gl.depthBufferBit)

            Gl.disable(Gl.cullFace)
            cam.position.set(camPos)
            cam.lookAt(Vec3.Zero)
            cam.resize(Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
            cam.update()
            val shader = OGShaders.texturePlane

            shader.bind()
            tmpTilesBuffer.texture.bind()
            shader.setUniformMatrix4("u_proj", cam.combined.`val`)
            tmpMat.setToScaling(tw * aspect, 1f, tw).translate(0f, offset, 0f)
            shader.setUniformMatrix4("u_trans", tmpMat.`val`)
            shader.setUniformi("u_texture0", 0)
            tilesMesh.render(shader, Gl.triangles)

            tilesMesh.render(shader, Gl.triangles)

        }

        //Draw.draw(Layer.background) { this.drawBackground() }
        Draw.draw(Layer.floor) { blocksL.floor.drawFloor() }
        Draw.draw(Layer.block - 1) { blocksL.drawShadows() }
        Draw.draw(Layer.block - 0.09f) {
            blocksL.floor.beginDraw()
            blocksL.floor.drawLayer(CacheLayer.walls)
        }

        Draw.drawRange(
            Layer.blockBuilding,
            { Draw.shader(Shaders.blockbuild, true) },
            { Draw.shader() })

        //render all matching environments
        /*
        for (renderer in envRenderers) {
            if ((renderer.env and Vars.state.rules.env) == renderer.env) {
                renderer.renderer.run()
            }
        }
        */

        if (Vars.state.rules.lighting && drawLight) {
            Draw.draw(Layer.light) { lights.draw() }
        }

        if (Vars.enableDarkness) {
            Draw.draw(Layer.darkness) { blocksL.drawDarkness() }
        }

        if (bloom != null) {
            bloom.resize(Core.graphics.width, Core.graphics.height)
            bloom.setBloomIntensity(Core.settings.getInt("bloomintensity", 6) / 4f + 1f)
            bloom.blurPasses = Core.settings.getInt("bloomblur", 1)
            Draw.draw(Layer.bullet - 0.02f) { bloom.capture() }
            Draw.draw(Layer.effect + 0.02f) { bloom.render() }
        }

        Vars.control.input.drawCommanded()

        Draw.draw(Layer.plans) { overlays.drawBottom() }

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

        val scaleFactor = 4f / Vars.renderer.displayScale

        //draw objective markers
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

        Draw.reset()

        Draw.draw(Layer.overlayUI) { overlays.drawTop() }
        if (Vars.state.rules.fog) Draw.draw(Layer.fogOfWar) { fog.drawFog() }
        Draw.draw(Layer.space) {
            if (launchAnimator != null && landTime > 0f) launchAnimator.drawLaunch()
        }
        if (launchAnimator != null) {
            Draw.z(Layer.space)
            launchAnimator.drawLaunchGlobalZ()
            Draw.reset()
        }

        Events.fire(Trigger.drawOver)
        blocksL.drawBlocks()

        Groups.draw.draw { obj: Drawc? -> obj!!.draw() }

        if (Vars.drawDebugHitboxes) {
            DebugCollisionRenderer.draw()
        }

        Draw.reset()
        Draw.flush()
        Draw.sort(false)

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

}
