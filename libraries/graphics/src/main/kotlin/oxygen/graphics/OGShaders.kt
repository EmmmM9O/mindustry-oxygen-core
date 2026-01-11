package oxygen.graphics

import arc.*
import arc.files.*
import arc.graphics.*
import arc.graphics.gl.*
import arc.math.geom.*
import arc.util.*
import mindustry.*
import oxygen.files.*

interface OShaderProcessor {
    fun preprocess(sourceO: String, fragment: Boolean, shader: OShader): String
}

object basicShaderChecker : OShaderProcessor {
    override fun preprocess(sourceO: String, fragment: Boolean, shader: OShader): String {
        var source = sourceO
        if (source.contains("#ifdef GL_ES")) {
            throw ArcRuntimeException("Shader contains GL_ES specific code; this should be handled by the preprocessor. Code: \n```\n $source \n```")
        }

        if (source.contains("#version")) {
            throw ArcRuntimeException("Shader contains explicit version requirement; this should be handled by the preprocessor. Code: \n```\n $source \n```")
        }
        return source
    }
}

object basicShaderEnd : OShaderProcessor {
    override fun preprocess(sourceO: String, fragment: Boolean, shader: OShader): String {
        var source = sourceO
        if (fragment) {
            source =
                "#ifdef GL_ES\n" +
                        "precision " + (if (source.contains("#define HIGHP") && !source.contains("//#define HIGHP")) "highp" else "mediump") + " float;\n" +
                        "precision mediump int;\n" +
                        "#else\n" +
                        "#define lowp  \n" +
                        "#define mediump \n" +
                        "#define highp \n" +
                        "#endif\n" + source
        } else {
            //strip away precision qualifiers
            source =
                "#ifndef GL_ES\n" +
                        "#define lowp  \n" +
                        "#define mediump \n" +
                        "#define highp \n" +
                        "#endif\n" + source
        }

        if (Core.gl30 != null) {
            val version =
                if (source.contains("#version ")) "" else
                    if (Core.app.isDesktop()) (if (Core.graphics.getGLVersion()
                            .atLeast(3, 2)
                    ) "150" else "130") else "300 es"
            return """#version $version 
                ${(if (fragment) "out${if (Core.app.isMobile()) " lowp" else ""} vec4 fragColor;\n" else "")}
                ${
                source
                    .replace("varying", if (fragment) "in" else "out")
                    .replace("attribute", if (fragment) "???" else "in")
                    .replace("texture2D(", "texture(")
                    .replace("textureCube(", "texture(")
                    .replace("gl_FragColor", "fragColor")
            }"""
        }
        return source
    }
}

open class IncludeProcessor : OShaderProcessor {
    var maxIncludeDepth: Int = 50
    val includeRegex = """^\s*#include\s+["<]([^">]+)[">]\s*(?://.*)?$""".toRegex(RegexOption.MULTILINE)
    open fun getInclude(name: String, base: Fi): Fi? =
        if (name.startsWith("/")) Vars.tree.get("shaders$name") else base.parent().sub(name)

    override fun preprocess(sourceO: String, fragment: Boolean, shader: OShader): String =
        processFile(sourceO, if (fragment) shader.fragFile else shader.vertFile, 0)

    private fun processFile(source: String, path: Fi, depth: Int): String {
        if (depth > maxIncludeDepth) {
            throw IllegalStateException("Maximum include depth ($maxIncludeDepth) exceeded")
        }
        return includeRegex.replace(source) { matchResult ->
            val name = matchResult.groupValues[1]
            val file = getInclude(name, path) ?: run {
                throw ArcRuntimeException("Could not find include file: <$name> from $path")
            }
            if (!file.exists())
                throw ArcRuntimeException("include file <$name> as $file not found from $path")
            processFile(file.readString(), file, depth + 1)
        }
    }
}

object includeProcessor : IncludeProcessor()

open class OShader(val vertFile: Fi, val fragFile: Fi) : Shader(vertFile, fragFile) {
    constructor(vert: String, frag: String) : this(
        OGShaders.getShaderFi("$vert.vert"),
        OGShaders.getShaderFi("$frag.frag")
    )

    val processors: List<OShaderProcessor> by lazy { createProcessors() }
    open fun createProcessors(): List<OShaderProcessor> = listOf(basicShaderChecker, includeProcessor, basicShaderEnd)
    override fun preprocess(sourceO: String, fragment: Boolean): String {
        var source = sourceO
        processors.forEach {
            source = it.preprocess(source, fragment, this)
        }
        return source
    }
}

class ShadowShader(vert: String, frag: String) : OShader(frag, vert) {
    var shadowMap: Texture? = null
    var lightMat: Mat3D? = null
    var lightDir: Vec3? = null
    override fun apply() {
        setUniformMatrix4("u_lightProj", lightMat!!.`val`)
        setUniformf("u_lightDir", lightDir!!)
        shadowMap!!.bind(1)
        setUniformi("u_shadowMap", 1)
        Gl.activeTexture(Gl.texture0);
    }
}

fun <T : Shader> T.setup(): T {
    this.init()
    return this
}

object OGShaders {
    fun getShaderFi(file: String) = Vars.tree.get("shaders/$file")!!
    val texturePlane: OShader by lazy { OShader("3d/simple", "3d/simpleUV").setup() }
    val zbatchShadow: ShadowShader by lazy { ShadowShader("batch/zbatchShadow", "batch/zbatchShadow").setup() }
    val solidDepth: OShader by lazy { OShader("3d/solidDepth", "3d/depth").setup() }
    val solid: ShadowShader by lazy { ShadowShader("3d/solid", "3d/solid").setup() }
}

