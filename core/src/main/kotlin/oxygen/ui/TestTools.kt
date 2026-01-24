package oxygen.ui

import arc.*
import arc.input.*
import arc.math.*
import arc.math.geom.*
import arc.scene.event.*
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.util.*
import mindustry.ui.*

import oxygen.*
import oxygen.graphics.bloom.*
import oxygen.graphics.g2d.*

class TestTools {
    val floatTable:FloatingTable

    init{
        floatTable = FloatingTable()
        floatTable.reset()
        floatTable.table{table->
            table.table().row()
            table.add(floatTable.dragButton).size(32f).center()
            table.table().row()
        }.uniformX().uniformY().height(32f).fill()
        floatTable.row()
        floatTable.table(Styles.black6){table->
            bloomSettings(table)
            rendererSettings(table)
        }.width(400f).height(400f)
        Core.scene.add(floatTable)
    }

    fun bloomSettings(table: Table){
        val bloom = Oxygen.renderer.obloom
        table.table{tab->
            addSlider(tab,"Threshold", 0f, 2f, 0.05f, bloom.threshold, {bloom.threshold = it})
            addSlider(tab,"Gamma", 0f, 3f, 0.05f, bloom.gamma, {bloom.gamma = it})
            addSlider(tab,"Intensity", 0f, 10f, 0.1f, bloom.intensity, {bloom.intensity = it})
            addSlider(tab,"Expousre",0f, 3f, 0.1f, bloom.exposure, {bloom.exposure = it})
            addSlider(tab,"Scale", 0f, 2f, 0.1f, bloom.scale, {bloom.scale = it})
            addSlider(tab,"mode",0f,10f,1f,bloom.mode.toFloat(),{bloom.mode = it.toInt()})
        }.grow().fill().row()
    }


    fun rendererSettings(table: Table){
        val renderer = Oxygen.renderer
        table.table{tab->
            addSlider(tab,"R", 0.5f, 2.5f, 0.05f, renderer.sclColor.r * COLOR_SCL, {renderer.sclColor.r = it/COLOR_SCL})
            addSlider(tab,"G", 0.5f, 2.5f, 0.05f, renderer.sclColor.g * COLOR_SCL, {renderer.sclColor.g = it/COLOR_SCL})
            addSlider(tab,"B", 0.5f, 2.5f, 0.05f, renderer.sclColor.b * COLOR_SCL, {renderer.sclColor.b = it/COLOR_SCL})
        }.grow().fill()
    }

    fun addSlider(table:Table,name:String, min:Float, max:Float, step:Float, value:Float, onChange:(value:Float)->Unit){
        val slider = Slider(min, max, step, false)
        slider.value = value
        val valueL = Label("", Styles.outlineLabel)
        val content = Table()
        content.add(name, Styles.outlineLabel).left().growX()
        content.add(valueL).padLeft(10f).right()
        content.touchable = Touchable.disabled
        slider.changed{
            onChange(slider.value)
            valueL.setText("%.2f".format(slider.value))
        }
        slider.change()
        table.add(slider).left().padLeft(10f).uniformY().width(200f).height(30f)
        table.add(content).right().uniformY()
        table.row()
    }

}
