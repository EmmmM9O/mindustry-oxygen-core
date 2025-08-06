package oxygen.core

import mindustry.mod.*
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
            repo = "github.com/EmmmM9O/mindustry-oxygen-core"
        )
        lateinit var modConfig: ModConfig
    }
}
