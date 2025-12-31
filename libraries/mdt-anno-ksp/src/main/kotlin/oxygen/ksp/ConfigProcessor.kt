package oxygen.ksp

import arc.util.serialization.*
import com.google.auto.service.*
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*
import kotlin.reflect.*

@AnnoResolver
fun resolveConfigDumpAnno(anno: KSAnnotation): ConfigDumpAnno = resolveConfigDumpAnnoGen(anno)

@AnnoResolver
fun resolveConfigDump(anno: KSAnnotation): ConfigDump = ConfigDump("", "")

@AnnoResolver
fun resolveModConfig(anno: KSAnnotation): ModConfig = resolveModConfigGen(anno)

@AutoService(SymbolProcessorProvider::class)
class ConfigProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        stepEProcess(environment) {
            annotatedAnno<ConfigDumpAnno> { elements ->
                fun process(d: AnnotatedAnnoData) {
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
                                dependencies = Dependencies(
                                    aggregating = false,
                                    element.containingFile!!, pair.first.containingFile!!
                                ),
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

                fun <T : Annotation> process(target: KClass<T>) {
                    annotatedAnnoDataFrom(target.qualifiedName!!, ConfigDumpAnno::class.qualifiedName!!, resolver)
                        ?.let {
                            process(it)
                        }
                }
                elements.forEach(::process)
                process(ModConfig::class)
                process(ConfigDump::class)
            }
        }
}
