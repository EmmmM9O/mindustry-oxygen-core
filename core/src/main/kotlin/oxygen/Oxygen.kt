package oxygen

import arc.*
import arc.files.*
import mindustry.*
import oxygen.annotations.*
import oxygen.core.*
import oxygen.graphics.*
import oxygen.util.*

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

    val renderer = ORenderer()

    fun init() {

    }

    fun getInternalFile(path: String): Fi = root.child(path)
}
