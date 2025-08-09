package oxygen

import arc.*
import arc.files.*
import arc.util.*
import mindustry.*
import oxygen.annotations.LOCATION_FILE_PATH
import oxygen.core.*
import oxygen.util.*

object Oxygen {
    val mod = Vars.mods.getMod(OCMain::class.java)!!
    val root = mod.root!!
    const val NAME = OCMain.NAME
    val modDir: Fi = Core.settings.dataDirectory.child("mods")
    val configDir: Fi = modDir.child("config").child(NAME)
    val dataDir: Fi = modDir.child("data").child(NAME)
    val log = OCMain.log
    val resolver = MdtAnnoResolver(log).apply {
        process(root.childPath(LOCATION_FILE_PATH).readString())
    }

    fun init() {

    }
}