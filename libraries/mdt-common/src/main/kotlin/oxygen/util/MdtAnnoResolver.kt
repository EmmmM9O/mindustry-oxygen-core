package oxygen.util

import arc.struct.*
import arc.util.serialization.*
import oxygen.annotations.*
import kotlin.reflect.*
import kotlin.reflect.full.*

class MdtAnnoResolver(val log: OLogger) {
    class LocationMap : ObjectMap<String, Seq<BaseLocationInfo>>()

    val json = object : Json() {
        @Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
        override fun <T : Any?> readValue(
            type: Class<T?>?,
            elementType: Class<*>?,
            jsonData: JsonValue,
        ): T? =

            when {
                BaseLocationInfo::class.java.isAssignableFrom(type) ->
                    super.readValue(
                        when (jsonData.get("type").asString()) {
                            "class" -> ClassLocationInfo::class.java
                            "property" -> PropertyLocationInfo::class.java
                            "declaration" -> DeclarationLocationInfo::class.java
                            "unknown" -> UnknownLocationInfo::class.java
                            else -> null
                        }, null, jsonData.get("value"), null
                    ) as T

                type == LocationMap::class.java -> {
                    val map = LocationMap()
                    var child = jsonData.child
                    while (child != null) {
                        map.put(
                            child.name,
                            super.readValue(Seq::class.java, BaseLocationInfo::class.java, child)
                                .`as`<BaseLocationInfo>()
                        )
                        child = child.next
                    }
                    map as T
                }

                else -> super.readValue(type, elementType, jsonData)
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveLocations(text: String): ObjectMap<String, Seq<BaseLocationInfo>> =
        json.fromJson(LocationMap::class.java, text)

    interface AnnoProcessor {
        fun process(locations: Seq<BaseLocationInfo>)
    }

    val processors = mapOf<String, AnnoProcessor>(
        ModConfig::class.java.name to object : AnnoProcessor {
            override fun process(locations: Seq<BaseLocationInfo>) {
                locations.forEach {
                    if (it !is PropertyLocationInfo || !it.isCompanion) {
                        log.atWarn {
                            mark(Marks.processor(ModConfig::class))
                            message("@ModConfig annotation should be applied to companion object property")
                        }
                        return
                    }
                    val parent = it.parent
                    log.atDebug {
                        mark(Marks.processor(ModConfig::class))
                        message("@ModConfig work at $parent for ${it.name}")
                    }
                    val clazz = Class.forName(parent).kotlin
                    val obj = clazz.companionObject!!
                    val instance = clazz.companionObjectInstance!!
                    val property = obj.memberProperties.first { p -> p.name == it.name } as KMutableProperty<*>
                    property.setter.call(instance, clazz.java.getField(it.name).getAnnotationsByType(ModConfig::class.java).first())
                }
            }
        }
    )

    fun process(text: String) {
        resolveLocations(text).let {
            processors.forEach { (name, processor) ->
                processor.process(it.get(name))
            }
        }
    }
}