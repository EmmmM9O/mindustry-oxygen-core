package oxygen.core

import arc.*
import arc.util.*
import mindustry.*
import mindustry.mod.*
import oxygen.*
import oxygen.graphics.*
import oxygen.graphics.g2d.*
import oxygen.util.*
import oxygen.util.Marks

class OCPreloader : Preloader() {
    companion object {
        val log: OLogger = logConfig { }.logger {
            logLevel(if (Log.level == Log.LogLevel.debug) Level.DEBUG else Level.INFO)
            name(null)
            arcHandlerAppender()
            simpleFormat {
                fun <T : Any> T?.defFormat() = formatOrEmpty { "[$it]" }
                template {
                    "$timeStr${
                        mark?.name().defFormat()
                    }${logger.name.defFormat()}:$message${cause.workOrEmpty { "\n:${throwableMsg(it)}" }}"
                }
                oxyTime { "<$mm-$ss>" }
            }
        }
        val omark = StrMark("Oxygen")
        var preloaded = false
    }

    init {
        preloaded = true
        Log.logger = Log.LogHandler { level, text ->
            log.at {
                level(level.toLevel())
                mark(Marks.log)
                message(text)
            }
        }
        log.atInfo {
            message("Oxygen preload")
            mark(omark)
        }
    }

    override fun preload() {

    }

    override fun beforeAll(app: ClientLauncher) {
        OGraphics.zbatch = ZSpriteBatch()
        Core.batch = OGraphics.zbatch
        log.atInfo {
            message("Replace batch")
            mark(omark)
        }
    }

    override fun modifyApplication(app: ClientLauncher) {
        Vars.renderer = Oxygen.renderer
        app.add(Oxygen.renderer)
        log.atInfo {
            message("Replace renderer")
            mark(omark)
        }
    }
}
