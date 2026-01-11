package oxygen.gradle

import org.gradle.api.model.*
import org.gradle.api.provider.*
import javax.inject.*

abstract class GlslExtension @Inject constructor(objects: ObjectFactory) {
    var shaderDir: Property<String> = objects.property(String::class.java).convention(DEFAULT_SHADER_DIR)
    var configFile: Property<String> = objects.property(String::class.java).convention(DEFAULT_CONFIG_FILE)
    var fileExtensions: ListProperty<String> = objects.listProperty(String::class.java).convention(defaultExtensions)
    var clangFormatPath: Property<String> = objects.property(String::class.java)
}
