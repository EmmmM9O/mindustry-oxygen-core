package oxygen.annotations

import arc.util.serialization.*
import java.lang.reflect.*
import kotlin.reflect.*

interface ConfigDumper {
    fun process(config: ConfigDumpAnno, data: Object, info: AnnotatedInfo, processor: Processor): String
    fun path(config: ConfigDumpAnno, data: Object, info: AnnotatedInfo, processor: Processor): String = config.path
}

enum class ConfigType {
    Unknown, Json
}

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigDumpAnno(
    val path: String,
    val type: ConfigType = ConfigType.Json,
    val dumper: KClass<out ConfigDumper> = ConfigDumper::class
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Default(val value: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AsType

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Recommended(val value: String)

class CustomConfigDumper : ConfigDumper {
    override fun path(config: ConfigDumpAnno, data: Object, info: AnnotatedInfo, processor: Processor): String =
        (data as Jval).get("path").asString()

    override fun process(config: ConfigDumpAnno, data: Object, info: AnnotatedInfo, processor: Processor): String =
        (data as Jval).get("text").asString()
}

@Retention(AnnotationRetention.RUNTIME)
@ConfigDumpAnno(emptyStr, ConfigType.Unknown, CustomConfigDumper::class)
@AnnoObject
annotation class ConfigDump(
    val path: String,
    val text: String
)

abstract class JsonDumper<T : Annotation>(val target: Class<T>) : ConfigDumper {
    override fun process(config: ConfigDumpAnno, data: Object, info: AnnotatedInfo, processor: Processor): String =
        (data as Jval).apply {
            check(this, processor)
            laterProcess(this, info, processor)
        }.toString(Jval.Jformat.formatted)

    abstract fun laterProcess(value: Jval, info: AnnotatedInfo, processor: Processor)

    fun check(value: Jval, processor: Processor) {
        val methodMap = target.declaredMethods.associateBy { it.name }
        val defaultMap = methodMap.keys.associateWith {
            isDefault(value.get(it), methodMap[it]!!)
        }.toMutableMap()
        if (target.isAnnotationPresent(Default::class.java)) {
            val map = Jval.read(target.getAnnotation(Default::class.java).value).asObject()
            map.forEach {
                val name = it.key
                defaultMap[name] =
                    value.get(name).toString(Jval.Jformat.formatted) == it.value.toString(Jval.Jformat.formatted)
            }
        }
        if (target.isAnnotationPresent(Recommended::class.java)) {
            val map = Jval.read(target.getAnnotation(Recommended::class.java).value).asObject()
            map.forEach {
                val name = it.key
                if (defaultMap[name] ?: true)
                    processor.info(
                        "Field $name received ${
                            value.get(name).toString(Jval.Jformat.formatted)
                        } of ${target.simpleName} is recommended due to ${it.value} but found default"
                    )
            }
        }
        defaultMap.forEach {
            if (it.value) value.remove(it.key)
        }
    }

    fun isDefault(value: Jval, field: Method): Boolean =
        when {
            value.isString -> value.asString() == field.defaultValue
            value.isBoolean -> value.asBool() == field.defaultValue
            value.isArray -> value.asArray().isEmpty
            value.isNumber -> value.asNumber() == field.defaultValue
            value.isObject -> value.asObject().isEmpty
            value.isNull -> false
            else -> true
        }
}
