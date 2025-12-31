package oxygen.graphics

import arc.*
import arc.assets.loaders.TextureLoader.TextureParameter
import arc.func.*
import arc.graphics.*
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.g2d.*
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
import kotlin.math.*

class OriRenderer : RendererI() {
    val customBackgrounds: ObjectMap<String, Runnable> = ObjectMap()

    var envRenderers: Seq<EnvRenderer> = Seq<EnvRenderer>()

    private val clearColor = Color(0f, 0f, 0f, 1f)

    //for landTime > 0: if true, core is currently *launching*, otherwise landing.
    private val camShakeOffset = Vec2()
    private var glErrors = 0
    var blocksL = OriBlockRenderer()

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
    }

    override fun addEnvRenderer(mask: Int, render: Runnable) {
        envRenderers.add(EnvRenderer(mask, render))
    }

    override fun addCustomBackground(name: String?, render: Runnable?) {
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
        }

        Core.camera.width = Core.graphics.width / camerascale
        Core.camera.height = Core.graphics.height / camerascale

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
                val message = when (error) {
                    Gl.invalidValue -> "invalid value"
                    Gl.invalidOperation -> "invalid operation"
                    Gl.invalidFramebufferOperation -> "invalid framebuffer operation"
                    Gl.invalidEnum -> "invalid enum"
                    Gl.outOfMemory -> "out of memory"
                    else -> "unknown error ($error)"
                }

                Log.err("[GL] Error: @", message)
                glErrors++
            }
        }

        PerfCounter.render.end()
    }

    override fun updateAllDarkness() {
        blocksL.updateDarkness()
        minimap.updateAll()
    }

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
        Events.fire(Trigger.preDraw)
        MapPreviewLoader.checkPreviews()

        Core.camera.update()

        if (java.lang.Float.isNaN(Core.camera.position.x) || java.lang.Float.isNaN(Core.camera.position.y)) {
            Core.camera.position.set(Vars.player)
        }

        Core.graphics.clear(clearColor)
        Draw.reset()

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

        Draw.draw(Layer.background) { this.drawBackground() }
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
        for (renderer in envRenderers) {
            if ((renderer.env and Vars.state.rules.env) == renderer.env) {
                renderer.renderer.run()
            }
        }

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

    private fun drawBackground() {
        //draw background only if there is no planet background with a skybox
        if (Vars.state.rules.backgroundTexture != null && (Vars.state.rules.planetBackground == null || !Vars.state.rules.planetBackground.drawSkybox)) {
            if (!Core.assets.isLoaded(Vars.state.rules.backgroundTexture, Texture::class.java)) {
                val file = Core.assets.fileHandleResolver.resolve(Vars.state.rules.backgroundTexture)

                //don't draw invalid/non-existent backgrounds.
                if (!file.exists() || !file.extEquals("png")) {
                    return
                }

                val desc = Core.assets.load(
                    Vars.state.rules.backgroundTexture,
                    Texture::class.java,
                    object : TextureParameter() {
                        init {
                            wrapV = TextureWrap.mirroredRepeat
                            wrapU = wrapV
                            minFilter = TextureFilter.linear
                            magFilter = minFilter
                        }
                    })

                Core.assets.finishLoadingAsset(desc)
            }

            val tex = Core.assets.get(Vars.state.rules.backgroundTexture, Texture::class.java)
            Tmp.tr1.set(tex)
            Tmp.tr1.u = 0f
            Tmp.tr1.v = 0f

            val ratio = Core.camera.width / Core.camera.height
            val size = Vars.state.rules.backgroundScl

            Tmp.tr1.u2 = size
            Tmp.tr1.v2 = size / ratio

            var sx = 0f
            var sy = 0f

            if (!Mathf.zero(Vars.state.rules.backgroundSpeed)) {
                sx = (Core.camera.position.x) / Vars.state.rules.backgroundSpeed
                sy = (Core.camera.position.y) / Vars.state.rules.backgroundSpeed
            }

            Tmp.tr1.scroll(sx + Vars.state.rules.backgroundOffsetX, -sy + Vars.state.rules.backgroundOffsetY)

            Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height)
        }

        if (Vars.state.rules.planetBackground != null) {
            val size = max(Core.graphics.width, Core.graphics.height)

            var resized = false
            if (backgroundBuffer == null) {
                resized = true
                backgroundBuffer = FrameBuffer(size, size)
            }

            if (resized || backgroundBuffer.resizeCheck(size, size)) {
                backgroundBuffer.begin(Color.clear)

                val params = Vars.state.rules.planetBackground

                //override some values
                params.viewW = size
                params.viewH = size
                params.alwaysDrawAtmosphere = true
                params.drawUi = false

                planets.render(params)

                backgroundBuffer.end()
            }

            val drawSize = max(Core.camera.width, Core.camera.height)
            Draw.rect(
                Draw.wrap(backgroundBuffer.texture),
                Core.camera.position.x,
                Core.camera.position.y,
                drawSize,
                -drawSize
            )
        }

        if (Vars.state.rules.customBackgroundCallback != null && customBackgrounds.containsKey(Vars.state.rules.customBackgroundCallback)) {
            customBackgrounds.get(Vars.state.rules.customBackgroundCallback).run()
        }
    }

    override fun showLanding(landCore: LaunchAnimator) {
        this.launchAnimator = landCore
        launching = false
        landTime = landCore.launchDuration()

        landCore.beginLaunch(false)
        camerascale = landCore.zoomLaunch()
    }

    override fun showLaunch(landCore: LaunchAnimator) {
        Vars.control.input.config.hideConfig()
        Vars.control.input.planConfig.hide()
        Vars.control.input.inv.hide()

        this.launchAnimator = landCore
        launching = true
        landTime = landCore.launchDuration()

        val music = landCore.launchMusic()
        music.stop()
        music.play()
        music.setVolume(Core.settings.getInt("musicvol") / 100f)

        landCore.beginLaunch(true)
    }

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

    class EnvRenderer(
        /** Environment bitmask; must match env exactly when and-ed.  */
        val env: Int,
        /** Rendering callback.  */
        val renderer: Runnable
    )

}
