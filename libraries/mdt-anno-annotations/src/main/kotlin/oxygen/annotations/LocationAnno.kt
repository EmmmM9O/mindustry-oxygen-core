package oxygen.annotations

import arc.util.serialization.*


@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LocationMark

abstract class BaseLocationInfo() {
    @JvmField
    var path: String = ""
    abstract fun type(): String
}

class UnknownLocationInfo : BaseLocationInfo() {
    override fun type(): String = "unknown"
}

open class DeclarationLocationInfo() : BaseLocationInfo() {
    @JvmField
    var name: String = ""
    override fun type(): String = "declaration"
}

class ClassLocationInfo() : DeclarationLocationInfo() {
    @JvmField
    var isNested: Boolean = false
    override fun type(): String = "class"
}

class PropertyLocationInfo() : DeclarationLocationInfo() {
    @JvmField
    var isCompanionObject: Boolean = false

    @JvmField
    var parent: ClassLocationInfo? = null
    override fun type(): String = "property"
}

fun writeLocation(json: Json, location: BaseLocationInfo) {
    json.writeObjectStart()
    json.writeValue("type", location.type())
    json.writer.name("value")
    json.writeObjectStart()
    json.writeFields(location)
    json.writeObjectEnd()
    json.writeObjectEnd()
}

fun readLocation(json: Json, value: JsonValue): BaseLocationInfo =
    json.readValue(
        when (value.get("type").asString()) {
            "class" -> ClassLocationInfo::class.java
            "property" -> PropertyLocationInfo::class.java
            "declaration" -> DeclarationLocationInfo::class.java
            "unknown" -> UnknownLocationInfo::class.java
            else -> null
        }, value.get("value")
    )