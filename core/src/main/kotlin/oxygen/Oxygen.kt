package oxygen

import arc.*
import arc.files.*
import arc.util.*
import mindustry.*
import oxygen.annotations.LOCATION_FILE_PATH
import oxygen.core.*
import oxygen.util.*
import oxygen.graphics.Renderer

object Oxygen {
    const val NAME = OCMain.NAME
    val mod = Vars.mods.getMod(NAME)!!
    val root = mod.root!!
    val modDir: Fi = Core.settings.dataDirectory.child("mods")
    val configDir: Fi = modDir.child("config").child(NAME)
    val dataDir: Fi = modDir.child("data").child(NAME)
    val log = OCPreloader.log
    var omark = OCPreloader.omark
    val resolver = MdtAnnoResolver(log).apply {
        process(root.childPath(LOCATION_FILE_PATH).readString())
    }

    val renderer = Renderer()

    fun init() {

    }
}
