package oxygen.ksp

import arc.struct.*
import arc.util.serialization.*
import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*

fun getLocation(element: KSAnnotated): BaseLocationInfo = when (element) {
    is KSClassDeclaration -> ClassLocationInfo().apply {
        name = element.simpleName.asString()
        path = element.qualifiedName!!.asString()
        isNested = element.parentDeclaration?.let { it is KSClassDeclaration } ?: false
    }

    is KSPropertyDeclaration -> PropertyLocationInfo().apply {
        name = element.simpleName.asString()
        path = element.qualifiedName!!.asString()
        parent = element.parentDeclaration?.let { getLocation(it) as? ClassLocationInfo }
        isCompanionObject = (element.parentDeclaration as? KSClassDeclaration)?.isCompanionObject ?: false
    }

    is KSDeclaration -> DeclarationLocationInfo().apply {
        name = element.simpleName.asString()
        path = element.qualifiedName!!.asString()
    }

    else -> UnknownLocationInfo().apply {
        path = element.toString()
    }
}

@AutoService(SymbolProcessorProvider::class)
class LocationProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = stepProcess(environment) {
        add(object : AnnotatedAnnotationStep(LocationMark::class.qualifiedName!!) {
            val map = ObjectMap<String, Seq<BaseLocationInfo>>()
            fun process(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                d: AnnotatedAnnoData
            ) {
                d.annotated.forEach { pair ->
                    map.get(pair.first.annotationType.resolve().declaration.qualifiedName!!.asString(), ::Seq)
                        .add(getLocation(pair.second))
                }
            }

            inline fun <reified T : Annotation> process(processor: BasicSymbolProcessor, resolver: Resolver) {
                annotatedAnnoDataFrom<T>(LocationMark::class.qualifiedName!!, resolver)
                    ?.let {
                        process(processor, resolver, it)
                    }
            }

            override fun actualProcess(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: Sequence<AnnotatedAnnoData>
            ): List<KSAnnotated> {
                elements.forEach { d ->
                    process(processor, resolver, d)
                }
                process<ModConfig>(processor, resolver)
                return emptyList()
            }

            override fun finish(processor: BasicSymbolProcessor) {
                processor.environment.codeGenerator.createNewFileByPath(
                    dependencies = Dependencies(false),
                    path = "location.json",
                    extensionName = ""
                ).use {
                    it.write(Jval.read(KspUtil.json.prettyPrint(map, JsonValue.PrettyPrintSettings().apply {
                        outputType = JsonWriter.OutputType.json
                    })).toString(Jval.Jformat.formatted).toByteArray())
                }
            }
        })
    }
}
