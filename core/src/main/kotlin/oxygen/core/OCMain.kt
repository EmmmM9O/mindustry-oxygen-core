package oxygen.core

import arc.*
import arc.util.*
import mindustry.game.*
import mindustry.mod.*
import oxygen.*
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
        fun fail(reason: String) {
            Events.run(EventType.ClientLoadEvent::class.java) {
                val errorDialog = ErrorDialog(reason)
                Time.run(10f) {
                    errorDialog.show()
                }
            }
        }
    }

    init {
        try {
            Class.forName("mindustry.mod.Preloader")
        } catch (e: ClassNotFoundException) {
            preloadFailed = true
            Log.err("mindustry.mod.Preloader not found.Please use Mindustry Oxygen")
            fail("@dialog.oxygen.error.preload")
        }
        if (!preloadFailed) Oxygen.init()
    }

    override fun init() {
        if (preloadFailed) {
            return
        }
    }

    override fun loadContent() {
        if (preloadFailed) {
            return
        }
        OBlocks.init()
    }
}
