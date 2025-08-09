pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
    }
}
gradle.settingsEvaluated {
    extra["rootProjectDir"] = rootProject.projectDir
}
rootProject.name = "oxygen"
listOf(
    "core",
    ":libraries",
    ":libraries:common",
    ":libraries:mdt-common",
    ":libraries:graphics",
    ":libraries:auto-util-annotations",
    ":libraries:auto-util-ksp",
    ":libraries:mdt-anno-annotations",
    ":libraries:mdt-anno-ksp",
    ":libraries:tools-anno-annotations",
    ":libraries:tools-anno-ksp"
)
    .forEach { include(it) }

