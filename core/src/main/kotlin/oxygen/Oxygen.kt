package oxygen

import arc.*
import arc.files.*
import arc.graphics.g3d.*
import arc.math.geom.*
import arc.util.*
import mindustry.*
import mindustry.game.*
import oxygen.annotations.*
import oxygen.core.*
import oxygen.graphics.*
import oxygen.ui.*
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

    val trans3D = Mat3D()

    val lightCam = Camera3D()
    val lightDir = Vec3(-1f, -1f, -4f).nor()

    val renderer = ORenderer()

    lateinit var testTools: TestTools
    fun init() {
        Events.run(EventType.ClientLoadEvent::class.java) {
            testTools = TestTools()
            Time.run(10f) {
                testTools.floatTable.setPosition(
                    Core.graphics.width.toFloat() / 2f,
                    Core.graphics.height.toFloat() / 1.5f
                )
            }
        }
    }

    fun getInternalFile(path: String): Fi = root.child(path)
}
