package oxygen.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import oxygen.util.*
import kotlin.reflect.*

inline fun <reified T : Annotation> KSAnnotated.getAnnotation() =
    annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == T::class.qualifiedName }

fun KSAnnotation.getAnnotationByName(name: String): KSAnnotation? =
    annotationType.resolve().declaration.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == name }

fun KSAnnotated.getAnnotationByName(name: String) =
    annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == name }

fun KSAnnotated.getAnnotationSub(target: String) =
    annotations.firstOrNull { it.isInheritedFrom(target) }

inline fun <reified T : Annotation> KSAnnotation.getAnnotation() =
    annotationType.resolve().declaration.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == T::class.qualifiedName }

fun <T : Annotation, U : KSType> U.getAnnotationC(clazz: KClass<T>) =
    annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == clazz.qualifiedName }

operator fun KSAnnotation.get(name: String) = arguments.firstOrNull { it.name?.asString() == name }?.value

inline fun <reified T> KSAnnotation.getV(name: String): T? = this[name]?.let { value ->
    try {
        when (value) {
            is List<*> -> value.toTypedArray() as T
            is KSType -> Class.forName(value.declaration.qualifiedName!!.asString()).kotlin as T
            is KSClassDeclaration -> anyToEnumOr<T>(value.simpleName.asString())
            else -> value as T
        }
    } catch (e: Throwable) {
        throw RuntimeException("Error while try to get $name", e)
    }
}

inline fun <reified T : Enum<T>> KSAnnotation.getEnum(name: String): T? = this[name]?.let { value ->
    when (value) {
        is KSClassDeclaration -> enumValueOf<T>(value.simpleName.asString())
        else -> null
    }
}

inline fun <reified T> KSAnnotation.getArr(name: String): Array<T>? = this[name]?.let { value ->
    when (value) {
        is Array<*> -> value.filterIsInstance<T>().toTypedArray<T>()
        is List<*> -> value.filterIsInstance<T>().toTypedArray<T>()
        else -> null
    }
}

inline fun <reified T> KSAnnotation.getOr(name: String, def: T) = getV(name) ?: def
inline fun <reified T> KSAnnotation.getThrow(name: String): T =
    getV(name)
        ?: throw IllegalArgumentException("Annotation field $name not found in ${annotationType.resolve().declaration.qualifiedName?.asString()}")

inline fun <reified T : Enum<T>> KSAnnotation.getEnumThrow(name: String): T =
    getEnum<T>(name)
        ?: throw IllegalArgumentException("Annotation enum field $name not found in ${annotationType.resolve().declaration.qualifiedName?.asString()}")

inline fun <reified T> KSAnnotation.getArrThrow(name: String): Array<T> =
    getArr<T>(name)
        ?: throw IllegalArgumentException("Annotation arr field $name not found in ${annotationType.resolve().declaration.qualifiedName?.asString()}")

inline fun <reified T> KSAnnotation.getSupplier(name: String, def: () -> T) = this[name] as? T ?: def()
fun KSAnnotation.isInheritedFrom(target: String): Boolean =
    (this.annotationType.resolve().declaration as KSClassDeclaration).isSubclassOf(target)

fun KSClassDeclaration.isSubclassOf(target: String): Boolean =
    if (this.qualifiedName?.asString() == target) true
    else let {
        superTypes.forEach { type ->
            val superClass = type.resolve().declaration
            if (superClass is KSClassDeclaration && superClass.isSubclassOf(target)) {
                true
            }
        }
        true
    }

inline fun <reified T : Annotation> annotatedAnnoDataFrom(ori:String,resolver: Resolver) = run {
    val target = T::class.qualifiedName!!
    resolver.getSymbolsWithAnnotation(target).takeIf { it.any() }
        ?.let {
            AnnotatedAnnotationStep.AnnotatedAnnoData(
                it.first().getAnnotationByName(target)!!.annotationType.resolve().declaration
                    .annotations.first { a -> a.annotationType.resolve().declaration.qualifiedName!!.asString() == ori},
                it.map { t -> t.getAnnotationByName(target)!! to t }
            )
        }
}