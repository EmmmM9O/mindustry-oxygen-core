package oxygen.annotations

import arc.util.serialization.*

class ModDumper : JsonDumper<ModConfig>(ModConfig::class.java) {
    override fun laterProcess(value: Jval, data: Object, info: AnnotatedInfo, processor: Processor) {
        value.put("main", (info.parent as DeclarationInfo).parentDeclaration!!.qualifiedName)
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@ConfigDumpAnno(
    "mod.json",
    ConfigType.Json, ModDumper::class
)
@AnnoObject
@LocationMark
@Recommended(
    """
    {
        "displayName" : "With a name is better",
        "author" : "With a author is better"
    }
"""
)
@Default(
    """
        {
            "texturescale":1.0
        }
    """
)
annotation class ModConfig(
    val name: String,
    val minGameVersion: String,
    val displayName: String = "",
    val author: String = "",
    val description: String = "",
    val version: String = "",
    val repo: String = "",
    val subtitle: String = "",
    val dependencies: Array<String> = [],
    val softDependencies: Array<String> = [],
    val pregenerated: Boolean = false,
    val hidden: Boolean = false,
    val keepOutlines: Boolean = false,
    val java: Boolean = true,
    val texturescale: Float = 1.0f,
    val contentOrder: String = "",
    val main: String = emptyStr
)
