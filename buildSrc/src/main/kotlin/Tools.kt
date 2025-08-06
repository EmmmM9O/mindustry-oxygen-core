import org.gradle.api.Project
import java.io.File

fun Project.packageName(): String {
    return "${rootProject.name}${path.replace(":", "-")}"
}

object Config {
    var rootDir: File? = null
    val properties by lazy {
        java.util.Properties().apply {
            File(rootDir!!, "gradle.properties").inputStream().use {
                load(it)
            }
        }
    }

    fun get(property: String): String {
        return properties.getProperty(property) ?: error("${property} not found in gradle.properties")
    }
}
