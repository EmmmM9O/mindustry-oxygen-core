package oxygen.graphics

import arc.graphics.gl.*
import mindustry.*

open class OShader : Shader {
    constructor(frag: String, vert: String) : super(
        OGShaders.getShaderFi("$vert.vert"),
        OGShaders.getShaderFi("$frag.frag")
    )
}

object OGShaders {
    val texturePlane: OShader by lazy { OShader("3d/simpleUV", "3d/simple") }
    fun getShaderFi(file: String) = Vars.tree.get("shaders/$file")!!
}

