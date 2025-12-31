import org.gradle.api.artifacts.dsl.*
import org.gradle.kotlin.dsl.*

enum class Modules(
    val projectName: String,
    val description: String,
    val path: String,
    val ksp: Boolean = false
) {
    CORE("core", "", ":core"),
    COMMON("common", "", ":libraries:common"),
    MDT_COMMON("mdt-common", "", ":libraries:mdt-common"),
    GRAPHICS("graphics", "", ":libraries:graphics"),
    AUTO_UTIL("auto-anno", "", ":libraries:auto-util-annotations"),
    AUTO_UTIL_KSP("auto-anno", "", ":libraries:auto-util-ksp"),
    MDT_ANNO("mdt-anno", "", ":libraries:mdt-anno-annotations"),
    MDT_ANNO_KSP("mdt-anno", "", ":libraries:mdt-anno-ksp", true),
    TOOLS_ANNO("tools-anno", "", ":libraries:tools-anno-annotations"),
    TOOLS_ANNO_KSP("tools-anno", "", ":libraries:tools-anno-ksp", true)
}

fun DependencyHandler.import(vararg modules: Modules) {
    modules.forEach { module ->
        add(if (module.ksp) "ksp" else "implementation", project(module.path))
    }
}

fun DependencyHandler.imp(vararg modules: Modules) {
    modules.forEach { module ->
        add("implementation", project(module.path))
    }
}

fun DependencyHandler.comp(vararg modules: Modules) {
    modules.forEach { module ->
        add("compileOnly", project(module.path))
    }
}

fun DependencyHandler.ksp(vararg modules: Modules) {
    modules.forEach { module ->
        add("ksp", project(module.path))
    }
}
