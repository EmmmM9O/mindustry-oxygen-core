package oxygen.graphics

import arc.graphics.*


class DepthFunc(val func: Int) {
    companion object {
        val never = DepthFunc(Gl.never)
        val less = DepthFunc(Gl.less)
        val equal = DepthFunc(Gl.equal)
        val lequal = DepthFunc(Gl.lequal)
        val notequal = DepthFunc(Gl.notequal)
        val greater = DepthFunc(Gl.greater)
        val gequal = DepthFunc(Gl.gequal)
        val always = DepthFunc(Gl.always)
    }

    fun apply() {
        Gl.depthFunc(func)
    }
}
