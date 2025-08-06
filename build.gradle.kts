import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import javax.management.RuntimeErrorException
import kotlin.apply

Config.rootDir = project.rootDir
fun getProperty(key: String): String? {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val localProperties = Properties().apply {
            load(localPropertiesFile.inputStream())
        }
        localProperties.getProperty(key)?.let { return it }
    }
    return project.findProperty(key) as? String
}

val sdkRoot: String? = getProperty("ANDROID_HOME")
val d8: String? = getProperty("D8")

plugins {
    java
    idea
    kotlin("jvm")
    id("com.google.devtools.ksp") apply false
}
allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "idea")
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        jvmToolchain(17)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
        sourceSets.main {
            kotlin.srcDirs("build/generated/oxy/main/kotlin")
        }
    }
    idea {
        module {
            sourceDirs = sourceDirs + file("build/generated/ksp/main/kotlin")
            generatedSourceDirs =
                generatedSourceDirs + file("build/generated/ksp/main/kotlin") + file("build/generated/ksp/test/kotlin")
        }
    }
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
    }
    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    }
    tasks {
        test{
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams=true
            }
            minHeapSize = "128m"
            maxHeapSize = "512m"
            filter {
                includeTestsMatching("*Test")
            }
        }
        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            archiveFileName = "${project.packageName()}-desktop.jar"
            from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
            from("build/generated/assets") { include("**") }
            from("assets/") { include("**") }
        }
        register("jarAndroid") {
            dependsOn("jar")
            doLast {
                try {
                    if (sdkRoot == null) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")
                    val platformRoot = File("$sdkRoot/platforms/").listFiles()?.sorted()?.reversed()?.find { f ->
                        File(
                            f,
                            "android.jar"
                        ).exists()
                    }
                    if (platformRoot == null) throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")
                    val dependencies =
                        (configurations.compileClasspath.get().files + configurations.runtimeClasspath.get().files + setOf(
                            File(platformRoot, "android.jar")
                        )).flatMap {
                            listOf("--classpath", it.toString())
                        }
                    project.exec {
                        workingDir(layout.buildDirectory.dir("libs"))
                        commandLine(
                            listOf(
                                d8 ?: "d8",
                                dependencies,
                                "--min-api",
                                "14",
                                "--output",
                                "${project.packageName()}-android.jar",
                                "${project.packageName()}-desktop.jar"
                            ).flatMap {
                                when (it) {
                                    is List<*> -> it
                                    else -> listOf(it)
                                }
                            }
                        )
                    }
                } catch (e: Throwable) {
                    println(e.message)
                    if (e is RuntimeErrorException) {
                        return@doLast
                    }
                    println("[ERROR]D8 unfounded")
                    delete(files("${layout.buildDirectory.get()}/libs/${project.packageName()}-android.jar"))
                }
            }
        }
        register("deploy", Jar::class) {
            dependsOn("jarAndroid")
            archiveFileName = "${project.packageName()}.jar"
            from(
                zipTree("${layout.buildDirectory.get()}/libs/${project.packageName()}-desktop.jar"),
                zipTree("${layout.buildDirectory.get()}/libs/${project.packageName()}-android.jar")
            )
            doLast {
                copy {
                    into(rootProject.layout.projectDirectory.dir("dist"))
                    from("${layout.buildDirectory.get()}/libs/${project.packageName()}.jar")
                }
            }
        }
    }
}
allprojects {
    group = "oxygen"
    version = "alpha"
}
