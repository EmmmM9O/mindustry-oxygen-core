package oxygen.core

import arc.*
import arc.util.*
import mindustry.game.*
import mindustry.mod.*
import oxygen.*
import oxygen.annotations.*
import oxygen.util.*

class OCMain : Mod() {
    companion object {
        const val NAME = "oxyc"
        const val DISPLAY_NAME = "Oxygen Core"

        @ModConfig(
            name = NAME,
            minGameVersion = "149",
            displayName = DISPLAY_NAME,
            author = "EmmmM9O",
            description = "Oxygen Core library",
            repo = "github.com/EmmmM9O/mindustry-oxygen-core"
        )
        @JvmField
        var modConfig: ModConfig = ModConfig(NAME, "")
        val log: OLogger = logConfig { }.logger {
            logLevel(if (Log.level == Log.LogLevel.debug) Level.DEBUG else Level.INFO)
            name(null)
            arcHandlerAppender()
            simpleFormat {
                fun <T : Any> T?.defFormat() = formatOrEmpty { "[$it]" }
                template {
                    "$timeStr${
                        mark?.name().defFormat()
                    }${logger.name.defFormat()}:$message\n${cause.workOrEmpty { ":${throwableMsg(it)}" }}"
                }
                oxyTime { "<$mm-$ss>" }
            }
        }
        val omark = StrMark("Oxygen")
    }

    init {
        Log.logger = Log.LogHandler { level, text ->
            log.at {
                level(level.toLevel())
                mark(Marks.log)
                message(text)
            }
        }
        Events.on(EventType.FileTreeInitEvent::class.java) {
            Oxygen.init()
            if (modConfig.minGameVersion.isEmpty()) {
                log.atError {
                    mark(omark)
                    message("@ModConfig init failed ,MdtAnnoProcessor do not work")
                }
                // TODO exception Dialog
            }
            log.atInfo {
                mark(omark)
                message("Oxygen Core init success")
            }
        }
    }

    override fun init() {
    }

}
