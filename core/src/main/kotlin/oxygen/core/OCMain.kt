package oxygen.core

import arc.util.*
import mindustry.mod.*
import oxygen.*
import oxygen.Oxygen.log
import oxygen.Oxygen.omark
import oxygen.annotations.*

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
    }

    override fun init() {
        if (preloadFailed) {
            return
        }
    }
}
