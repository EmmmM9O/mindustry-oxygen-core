package oxygen.ksp

import com.google.auto.service.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*
import oxygen.annotations.*
import oxygen.util.*

const val annoT = "anno"

class AnnoMarkProcessor(environment: SymbolProcessorEnvironment) : BasicSymbolProcessor(environment) {

    override val steps: Set<Step>
        get() = processSteps {

            individual<AnnoObject> { processor, resolver, elements ->
                elements.filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == ClassKind.ANNOTATION_CLASS }
                    .forEach { anno ->
                        val data = anno.getAnnotation<AnnoObject>() ?: run {
                            processor.error("${anno.qualifiedName?.asString()} has no AnnoResolver but received to process")
                            return@forEach
                        }
                        val file = anno.containingFile ?: run {
                            processor.error("Source file of function ${anno.qualifiedName?.asString()} not found")
                            return@forEach
                        }
                        val name = data.getThrow<String>("name").replace("@", anno.simpleName.asString())
                        val funcName = data.getThrow<String>("funcName").replace("@", anno.simpleName.asString())
                        val gloFuncName = data.getThrow<String>("gloFuncName").replace("@", anno.simpleName.asString())
                        val packageP = data.getThrow<String>("path").replace("@", anno.packageName.asString())
                        val annoObj = ClassName(packageP, name)
                        FileSpec.builder(
                            packageP,
                            data.getThrow<String>("file")
                                .capitalize()
                                .replace("@", anno.simpleName.asString())
                        )
                            .addType(
                                TypeSpec.classBuilder(name)
                                    .addOriginatingKSFile(file)
                                    .addAnnotation(
                                        AnnotationSpec.builder(Source::class)
                                            .addMember("source = %S", anno.qualifiedName?.asString() ?: "")
                                            .build()
                                    )
                                    .addModifiers(KModifier.DATA)

                                    .addProperties(anno.getAllProperties().map {
                                        PropertySpec.builder(it.simpleName.asString(), it.type.toTypeName())
                                            .initializer(it.simpleName.asString()).build()
                                    }.asIterable())
                                    .primaryConstructor(
                                        FunSpec.constructorBuilder()
                                            .apply {
                                                anno.getAllProperties().forEach {
                                                    addParameter(
                                                        it.simpleName.asString(),
                                                        it.type.toTypeName(),
                                                    )
                                                }
                                            }.build()
                                    )
                                    .build()
                            )
                            .addFunction(
                                FunSpec.builder(funcName)
                                    .addOriginatingKSFile(file)
                                    .receiver(anno.asStarProjectedType().toTypeName())
                                    .returns(annoObj)
                                    .apply {
                                        addStatement(StringBuilder().apply {
                                            append("return $name(")
                                            anno.getAllProperties().forEach {
                                                append("this.${it.simpleName.asString()},")
                                            }
                                            append(")")
                                        }.toString())

                                    }
                                    .build()
                            )
                            .addFunction(
                                FunSpec.builder(gloFuncName)
                                    .addOriginatingKSFile(file)
                                    .returns(annoObj)
                                    .addParameter("anno", anno.asStarProjectedType().toTypeName())
                                    .addStatement("return anno.$funcName()")
                                    .build()
                            )
                            .build().writeTo(processor.environment.codeGenerator, aggregating = false, listOf(file))
                        processor.debug("AnnoObject Generate object $name , $funcName")
                    }
                emptyList()
            }
            individual<AnnoResolver> { processor, resolver, elements ->
                elements.filterIsInstance<KSFunctionDeclaration>()
                    .forEach { function ->
                        val anno = function.returnType?.resolve() ?: run {
                            processor.error("Return type of function ${function.qualifiedName?.asString()} must be a annotation")
                            return@forEach
                        }
                        val declaration = anno.declaration
                        if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ANNOTATION_CLASS) {
                            val file = function.containingFile ?: run {
                                processor.error("Source file of function ${function.qualifiedName?.asString()} not found")
                                return@forEach
                            }

                            val data = function.getAnnotation<AnnoResolver>() ?: run {
                                processor.error("$function has no AnnoResolver but received to process")
                                return@forEach
                            }
                            val name = data.getThrow<String>("name").replace("@", function.simpleName.asString())
                            FileSpec.builder(
                                data.getThrow<String>("path").replace("@", function.packageName.asString()),
                                data.getThrow<String>("file")
                                    .capitalize()
                                    .replace("@", function.simpleName.asString())
                            )
                                .addFunction(
                                    FunSpec.builder(name)
                                        .returns(ClassName.bestGuess(declaration.qualifiedName!!.asString()))
                                        .addOriginatingKSFile(file)
                                        .addParameter(annoT, KSAnnotation::class)
                                        .addAnnotation(
                                            AnnotationSpec.builder(Source::class)
                                                .addMember("source = %S", function.qualifiedName?.asString() ?: "")
                                                .build()
                                        )
                                        .addStatement(
                                            StringBuilder().apply {
                                                append("return ${declaration.simpleName.asString()}(\n")
                                                declaration.getAllProperties().forEach { param ->
                                                    val tType = param.type.resolve()
                                                    val declaration = tType.declaration

                                                    val method = when {
                                                        (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS) -> "getEnumThrow"
                                                        declaration.qualifiedName?.asString() == "kotlin.Array" -> "getArrThrow"
                                                        else -> "getThrow"
                                                    }
                                                    //val method = "getThrow"
                                                    val type = when {
                                                        declaration.qualifiedName?.asString() == "kotlin.Array" -> tType.arguments.firstOrNull()!!.type!!.resolve().declaration.qualifiedName!!.asString()
                                                        else -> param.type.toTypeName()
                                                    }
                                                    append("    $annoT.$method<${type}>(\"${param.simpleName.asString()}\"),\n")

                                                }
                                                append(")")
                                            }.toString()
                                        )
                                        .build()
                                )
                                .apply {
                                    declaration.annotations.forEach { tanno ->
                                        when (tanno.annotationType.resolve().declaration.qualifiedName!!.asString()) {
                                            AnnoObject::class.qualifiedName -> {
                                                val nameR = tanno.getThrow<String>("name")
                                                    .replace("@", declaration.simpleName.asString())
                                                val funcName = tanno.getThrow<String>("funcName")
                                                    .replace("@", declaration.simpleName.asString())
                                                val packageP = tanno.getThrow<String>("path")
                                                    .replace("@", declaration.packageName.asString())
                                                val annoObj = ClassName(packageP, nameR)
                                                addFunction(
                                                    FunSpec.builder("${name}Obj")
                                                        .returns(annoObj)
                                                        .addOriginatingKSFile(file)
                                                        .addParameter(annoT, KSAnnotation::class)
                                                        .addAnnotation(
                                                            AnnotationSpec.builder(Source::class)
                                                                .addMember(
                                                                    "source = %S",
                                                                    function.qualifiedName?.asString() ?: ""
                                                                )
                                                                .build()
                                                        )
                                                        .addStatement("return $name($annoT).$funcName()")
                                                        .build()
                                                )
                                                addImport(packageP, funcName)
                                            }
                                        }
                                    }
                                }
                                .build().writeTo(processor.environment.codeGenerator, aggregating = false, listOf(file))
                            processor.debug("AnnoResolver Generate function $name")
                        } else {
                            processor.error("Return type of function ${function.qualifiedName?.asString()} must be a annotation")
                        }
                    }
                emptyList()
            }

        }
}

@AutoService(SymbolProcessorProvider::class)
class AnnoMarkProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = AnnoMarkProcessor(environment)
}
