package oxygen.ksp

import arc.struct.*
import arc.util.serialization.*
import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*
import kotlin.reflect.*

fun getLocation(element: KSAnnotated): BaseLocationInfo = when (element) {
    is KSClassDeclaration -> ClassLocationInfo().apply {
        name = element.simpleName.asString()
        path = element.qualifiedName!!.asString()
    }

    is KSPropertyDeclaration -> PropertyLocationInfo().apply {
        name = element.simpleName.asString()
        path = element.qualifiedName!!.asString()
        isCompanion = (element.parentDeclaration as? KSClassDeclaration)?.isCompanionObject ?: false
        parent = if (isCompanion)
            element.parentDeclaration?.parentDeclaration?.qualifiedName?.asString()!!
        else element.parentDeclaration?.qualifiedName?.asString()!!
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
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = stepEProcess(environment) {
        val map = ObjectMap<String, Seq<BaseLocationInfo>>()
        annotatedAnno<LocationMark> { elements ->
            fun process(d: AnnotatedAnnoData) {
                d.annotated.forEach { pair ->
                    map.get(pair.first.annotationType.resolve().declaration.qualifiedName!!.asString(), ::Seq)
                        .add(getLocation(pair.second))
                }
            }

            fun <T : Annotation> process(target: KClass<T>) {
                annotatedAnnoDataFrom(target.qualifiedName!!, LocationMark::class.qualifiedName!!, resolver)
                    ?.let {
                        process(it)
                    }
            }
            elements.forEach(::process)
            process(ModConfig::class)
        }.finish {
            processor.environment.codeGenerator.createNewFileByPath(
                dependencies = Dependencies(false),
                path = LOCATION_FILE_PATH,
                extensionName = ""
            ).use {
                it.write(Jval.read(KspUtil.json.prettyPrint(map, JsonValue.PrettyPrintSettings().apply {
                    outputType = JsonWriter.OutputType.json
                })).toString(Jval.Jformat.formatted).toByteArray())
            }
        }
    }
}
