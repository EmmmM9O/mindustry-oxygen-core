package oxygen.ui.dialogs

import arc.*
import arc.graphics.*
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.struct.*
import arc.util.*
import mindustry.game.EventType.*
import mindustry.gen.*
import mindustry.graphics.*
import mindustry.ui.*
import mindustry.ui.dialogs.*

class ErrorDialog(val content:String) : BaseDialog("@dialog.oxygen.error.title") {
    init{
        shown(this::setup)
        onResize(this::setup)
    }

    fun setup(){
        cont.clear()
        buttons.clear()
        cont.table{tab->
            tab.add(content).fillX().wrap().get().setAlignment(Align.center);
        }.grow().row()
        addCloseButton()
    }
}
