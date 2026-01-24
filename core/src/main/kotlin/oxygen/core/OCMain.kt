package oxygen.core

import arc.*
import arc.util.*
import mindustry.game.*
import mindustry.mod.*
import oxygen.*
import oxygen.Oxygen.log
import oxygen.Oxygen.omark
import oxygen.annotations.*
import oxygen.content.*
import oxygen.ui.dialogs.*

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
            repo = "github.com/EmmmM9O/mindustry-oxygen-core",
            preloader = "oxygen.core.OCPreloader",
        )
        @JvmField
        var modConfig: ModConfig = ModConfig(NAME, "")
        var preloadFailed = false
    }

    init {
        if (!OCPreloader.preloaded) {
            Log.err("Oxygen preload fail!Please make sure you are using Mindustry Oxygen client!")
            preloadFailed = true
            OCPreloader.failReason = "@dialog.oxygen.error.preload"
        } else if (OCPreloader.error) {
            // Logged in preloader
            preloadFailed = true
        } else {
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

        if (preloadFailed) {
            Events.run(EventType.ClientLoadEvent::class.java) {
                val errorDialog = ErrorDialog(OCPreloader.failReason)
                Time.run(10f) {
                    errorDialog.show()
                }
            }
        }
    }

    override fun init() {
        if (preloadFailed) {
            return
        }
    }

    override fun loadContent() {
        OBlocks.init()
    }
}
