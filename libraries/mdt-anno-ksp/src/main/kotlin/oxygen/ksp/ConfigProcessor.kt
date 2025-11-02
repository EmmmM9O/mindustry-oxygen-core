package oxygen.ksp

import arc.util.serialization.*
import com.google.auto.service.*
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*
import kotlin.reflect.*
import kotlin.sequences.*

@AnnoResolver
fun resolveConfigDumpAnno(anno: KSAnnotation): ConfigDumpAnno = resolveConfigDumpAnnoGen(anno)

@Deprecated("")
class ConfigDumpStep<T : Annotation, O : Any>(
    target: KClass<T>,
    val resolver: (anno: KSAnnotation) -> O
) : Step {
    val name = target.qualifiedName!!
    override fun annotations(): Set<String> = setOf(name)
    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> {
        elements.values.first().forEach { element ->
            val anno =
                element.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == name }
                    ?: run {
                        processor.error("$element has no $name but received to process")
                        return@forEach
                    }
            val configDumperAnno = anno.getAnnotation<ConfigDumpAnno>() ?: run {
                processor.error("$name must have @ConfigDumpAnno to use ConfigDumpProcessor")
                return@forEach
            }
            val dumpData = resolveConfigDumpAnno(configDumperAnno)
            try {
                val annoData = resolver(anno) as Object

                val dumper = (dumpData.dumper.java.constructors[0].newInstance() ?: run {
                    processor.error("Dumper ${dumpData.dumper.qualifiedName} must have a constructor()")
                    return@forEach
                }) as ConfigDumper
                val info = getInfoAnnotated(element)
                val pro = ProcessorImpl(processor)
                val path = dumper.path(dumpData, annoData, info, pro)
                val data = dumper.process(dumpData, annoData, info, pro)
                processor.debug("$name With Dumper ${dumpData.dumper.qualifiedName} Generate To $path with\n$data")
                processor.environment.codeGenerator.createNewFileByPath(
                    dependencies = Dependencies(false),
                    path = path,
                    extensionName = ""
                ).use {
                    it.write(data.toByteArray())
                }
            } catch (e: Throwable) {
                processor.error(e.toString())
                processor.logger.atError {
                    message("")
                    cause(e)
                }
                e.printStackTrace()
                return@forEach
            }
        }
        return emptyList()
    }
}

@Suppress("DEPRECATION")
@Deprecated("")
fun <T : Annotation, U : Any> StepsMarker.configDump(
    clazz: KClass<T>,
    func: (anno: KSAnnotation) -> U
) {
    set.add(ConfigDumpStep(clazz, func))
}

@AnnoResolver
fun resolveConfigDump(anno: KSAnnotation): ConfigDump = ConfigDump("", "")

@AnnoResolver
fun resolveModConfig(anno: KSAnnotation): ModConfig = resolveModConfigGen(anno)

@AutoService(SymbolProcessorProvider::class)
class ConfigProcessorProvider : SymbolProcessorProvider {
    fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        d: AnnotatedAnnotationStep.AnnotatedAnnoData
    ) {
        val name = d.oriAnno.annotationType.resolve().declaration.qualifiedName!!.asString()
        val dumpData = resolveConfigDumpAnno(d.oriAnno)
        d.annotated.forEach { pair ->
            val element = pair.second
            val annoData =
                Jval.newObject().apply {
                    pair.first.arguments.forEach {
                        put(it.name!!.asString(), jvalFrom(it.value))
                    }
                } as Object
            val dumper = (dumpData.dumper.java.constructors[0].newInstance() ?: run {
                processor.error("Dumper ${dumpData.dumper.qualifiedName} must have a constructor()")
                return@forEach
            }) as ConfigDumper
            val info = getInfoAnnotated(element)
            val pro = ProcessorImpl(processor)
            val path = dumper.path(dumpData, annoData, info, pro)
            val data = dumper.process(dumpData, annoData, info, pro)
            try {
                processor.debug("$name With Dumper ${dumpData.dumper.qualifiedName} Generate To $path with\n$data")
                processor.environment.codeGenerator.createNewFileByPath(
                    dependencies = Dependencies(aggregating = false,
                        element.containingFile!!,pair.first.containingFile!!),
                    path = path,
                    extensionName = ""
                ).use {
                    it.write(data.toByteArray())
                }
            } catch (e: Throwable) {
                processor.error(e.toString())
                processor.logger.atError {
                    message("")
                    cause(e)
                }
                e.printStackTrace()
                return@forEach
            }
        }
    }

    inline fun <reified T : Annotation> process(processor: BasicSymbolProcessor, resolver: Resolver) {
        annotatedAnnoDataFrom<T>(ConfigDumpAnno::class.qualifiedName!!, resolver)
            ?.let {
                process(processor, resolver, it)
            }
    }

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        stepProcess(environment) {
            annotatedAnno<ConfigDumpAnno> { processor, resolver, elements ->
                elements.forEach { d ->
                    process(processor, resolver, d)
                }
                process<ModConfig>(processor, resolver)
                process<ConfigDump>(processor, resolver)
                emptyList()
            }
        }
}
