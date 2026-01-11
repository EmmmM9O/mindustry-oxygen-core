package oxygen.content

import arc.graphics.*
import arc.graphics.gl.*
import mindustry.content.*
import mindustry.gen.*
import mindustry.type.*
import mindustry.type.ItemStack.with
import mindustry.world.*
import mindustry.world.meta.*
import oxygen.*
import oxygen.Oxygen.lightCam
import oxygen.Oxygen.lightDir
import oxygen.Oxygen.renderer
import oxygen.graphics.*
import oxygen.util.*
import oxygen.world.blocks.*

open class CubeBlock(name: String) : Block(name) {
    val cubeMesh = Meshes.solidCubeMesh(24f, 24f, 12f)
    val depthShader = OGShaders.solidDepth
    val objShader = OGShaders.solid

    init {
        solid = true
        destructible = true
        canOverdrive = false
        drawDisabled = false

        envEnabled = Env.any
    }

    public inner class CubeBuild() : Building(), G3DrawBuilding {
        fun prepare(shader: Shader) {
            shader.bind()
            OTmp.m1.set(Oxygen.trans3D).translate(x, y, 0f)
            shader.setUniformMatrix4("u_trans", OTmp.m1.`val`)
            shader.setUniformMatrix4("u_proj", OGraphics.proj3D().`val`)
            shader.apply()
        }

        override fun draw3D() {
            objShader.lightMat = lightCam.combined
            objShader.lightDir = lightDir
            objShader.shadowMap = renderer.shadowBuffer.texture
            objShader.setUniformf("u_camPos", lightCam.position)
            prepare(objShader)
            cubeMesh.render(objShader, Gl.triangles)
        }

        override fun drawDepth() {
            prepare(depthShader)
            cubeMesh.render(depthShader, Gl.triangles)
        }

        override fun draw() {
        }
    }
}

object OBlocks {
    //Test
    lateinit var cube: Block
    fun init() {
        cube = object : CubeBlock("cube") {
            init {
                requirements(Category.defense, with(Items.copper, 1))
                health = 45
                size = 3
            }
        }
    }
}
