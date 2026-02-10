package oxygen.ui

import arc.*
import arc.scene.event.*
import arc.scene.ui.*
import arc.scene.ui.layout.*
import mindustry.gen.*
import mindustry.ui.*
import oxygen.*
import oxygen.graphics.bloom.*
import oxygen.graphics.g2d.*

class TestTools {
    val floatTable: FloatingTable
    val bloomLabel: Label
    val contentTable: Table
    val visButton: ImageButton
    var visible = true

    init {
        floatTable = FloatingTable()
        floatTable.reset()
        visButton = ImageButton(ImageButton.ImageButtonStyle(Styles.cleari).apply {
            imageUp = Icon.eyeSmall
            imageChecked = Icon.eyeOffSmall
        })
        visButton.changed {
            visible = !visible
            contentTable.visible = visible
        }
        bloomLabel = Label("None", Styles.outlineLabel)
        floatTable.table { table ->
            table.table().row()
            table.table { buttons ->
                buttons.add(floatTable.dragButton).size(32f)
                buttons.add(visButton).padLeft(10f).size(32f)
                buttons.button(Icon.starSmall, Styles.cleari) {
                    val bloom = Oxygen.renderer.obloom
                    if (bloom is CompareBloom) {
                        bloom.enableTest = !bloom.enableTest
                    }
                }
                buttons.add(bloomLabel).height(32f)
            }.height(32f).center()
            table.table().row()
        }.uniformX().uniformY().height(32f).fill()
        floatTable.row()
        contentTable = Table(Styles.black6).apply {
            bloomSettings(this)
            rendererSettings(this)
        }
        floatTable.add(contentTable).width(400f).height(400f)
        Core.scene.add(floatTable)
    }

    fun bloomSettings(table: Table) {
        val bloom = Oxygen.renderer.obloom
        table.table { tab ->
            addSlider(tab, "Threshold", 0f, 2f, 0.05f, bloom.threshold, { bloom.threshold = it })
            addSlider(tab, "Gamma", 0f, 3f, 0.05f, bloom.gamma, { bloom.gamma = it })
            addSlider(tab, "Intensity", 0f, 3f, 0.05f, bloom.intensity, { bloom.intensity = it })
            addSlider(tab, "Expousre", 0f, 3f, 0.1f, bloom.exposure, { bloom.exposure = it })
            addSlider(tab, "Scale", 0f, 2f, 0.1f, bloom.scale, { bloom.scale = it })
            addSlider(tab, "mode", 0f, 10f, 1f, bloom.mode.toFloat(), { bloom.mode = it.toInt() })
            if (bloom is CompareBloom) {
                addSlider(tab, "select", 0f, 3f, 1f, bloom.choice.toFloat(), {
                    bloom.choice = it.toInt()
                    if (bloom.choice == 0) bloomLabel.setText("None")
                    else if (bloom.choice <= 3) bloomLabel.setText("${bloom.blooms[bloom.choice - 1]::class.simpleName}")

                })
            }
        }.grow().fill().row()
    }


    fun rendererSettings(table: Table) {
        val renderer = Oxygen.renderer
        table.table { tab ->
            addSlider(
                tab,
                "R",
                0.5f,
                2.5f,
                0.05f,
                renderer.sclColor.r * COLOR_SCL,
                { renderer.sclColor.r = it / COLOR_SCL })
            addSlider(
                tab,
                "G",
                0.5f,
                2.5f,
                0.05f,
                renderer.sclColor.g * COLOR_SCL,
                { renderer.sclColor.g = it / COLOR_SCL })
            addSlider(
                tab,
                "B",
                0.5f,
                2.5f,
                0.05f,
                renderer.sclColor.b * COLOR_SCL,
                { renderer.sclColor.b = it / COLOR_SCL })
        }.grow().fill()
    }

    fun addSlider(
        table: Table,
        name: String,
        min: Float,
        max: Float,
        step: Float,
        value: Float,
        onChange: (value: Float) -> Unit
    ) {
        val slider = Slider(min, max, step, false)
        slider.value = value
        val valueL = Label("", Styles.outlineLabel)
        val content = Table()
        content.add(name, Styles.outlineLabel).left().growX()
        content.add(valueL).padLeft(10f).right()
        content.touchable = Touchable.disabled
        slider.changed {
            onChange(slider.value)
            valueL.setText("%.2f".format(slider.value))
        }
        slider.change()
        table.add(slider).left().padLeft(10f).uniformY().width(200f).height(30f)
        table.add(content).right().uniformY()
        table.row()
    }

}
