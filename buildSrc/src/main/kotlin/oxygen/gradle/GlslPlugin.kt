package oxygen.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.*
import org.gradle.kotlin.dsl.*

import java.io.*
const val DEFAULT_SHADER_DIR = "assets/shaders"
const val DEFAULT_CONFIG_FILE = ".clang-format"
val defaultExtensions = listOf("vert", "frag", "glsl")

abstract class GlslPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("glsl",GlslExtension::class.java)
        project.tasks.register<ShaderFormatTask>("glslFormat") {
            group = "formatting"
            description = "Formats GLSL shaders using clang-format"

            checkOnly.set(false)
            configureFromExtension(extension)
        }

        project.tasks.register<ShaderFormatTask>("glslCheck") {
            group = "verification"
            description = "Checks GLSL shaders for formatting errors using clang-format"

            checkOnly.set(true)
            configureFromExtension(extension)
        }
    }
}


abstract class ShaderFormatTask : DefaultTask() {
    @get:Internal
    abstract val shaderDir: DirectoryProperty
    
    @get:InputFile
    abstract val configFile: RegularFileProperty
    
    @get:Input
    abstract val checkOnly: Property<Boolean>
    
    @get:Input
    abstract val fileExtensions: ListProperty<String>

    @get:Internal
    abstract val clangFormatPath: Property<String>
    
    init {
//        inputs.dir(shaderDir)//.withPathSensitivity(PathSensitivity.RELATIVE).optional()
//        inputs.file(configFile)//.withPathSensitivity(PathSensitivity.RELATIVE).optional()
//        inputs.property("extensions", extensions)
//        inputs.property("clangFormatPath", clangFormatPath)
        outputs.upToDateWhen { checkOnly.get() }
    }
    
    fun configureFromExtension(ext: GlslExtension) {
        shaderDir.set(project.layout.projectDirectory.dir(ext.shaderDir.get()))
        configFile.set(project.rootProject.layout.projectDirectory.file(ext.configFile.get()))
        fileExtensions.set(ext.fileExtensions.get())
        clangFormatPath.set(ext.clangFormatPath.orElse(
            System.getenv("CLANG_FORMAT_PATH") ?: "clang-format"
        ).get())
    }
    
    @TaskAction
    fun executeTask() {
        val dir = shaderDir.get().asFile
        if (!dir.exists()) {
            logger.lifecycle("${project.name}: Shader directory not exists - ${dir.relativeTo(project.rootDir)}")
            return
        }
        
        val shaderFiles = findShaderFiles(dir)
        if (shaderFiles.isEmpty()) {
            logger.lifecycle("${project.name}: No shader file in $dir with {${fileExtensions.get().joinToString(",")}} skip")
            return
        }
        
        logger.lifecycle("${project.name}: Found ${shaderFiles.size} glsl file")
        
        val clangFormat = clangFormatPath.get()
        
        if (!checkClangFormat(clangFormat)) {
            throw GradleException(
                "clang-format not found \n" +
                "1. Install clang-format\n" +
                "2. Add to PATH\n" +
                "3. Or set CLANG_FORMAT_PATH\n" +
                "4. or add clangFormatPath to glsl property"
            )
        }
        
        val results = processShaderFiles(clangFormat, shaderFiles)
        printResults(results)
        
        if (checkOnly.get() && results.failedFiles.isNotEmpty()) {
            throw GradleException("${results.failedFiles.size} shader file uncorrect")
        }
    }
    
    private fun findShaderFiles(baseDir: File): List<File> {
        return project.fileTree(baseDir) {
            fileExtensions.get().forEach {
                include("**/*.$it")
            }
        }.files.toList()
    }
    
    private fun checkClangFormat(path: String): Boolean {
        return try {
            val process = ProcessBuilder(path, "--version").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun processShaderFiles(clangFormat: String, files: List<File>): ProcessResults {
        val formatted = mutableListOf<File>()
        val failed = mutableListOf<Pair<File, String>>()
        val needsFormat = mutableListOf<File>()
        
        val configFile = configFile.orNull?.asFile
        val styleArg = if (configFile != null && configFile.exists()) {
            "--style=file:${configFile.absolutePath}"
        } else {
            "--style=file"
        }
        
        files.forEach { file ->
            try {
                val args = mutableListOf(clangFormat, styleArg)
                
                if (checkOnly.get()) {
                    args.addAll(listOf("--dry-run", "--Werror"))
                } else {
                    args.add("-i")
                }
                
                args.add(file.absolutePath)
                
                val process = ProcessBuilder(args).start()
                val exitCode = process.waitFor()
                val errorOutput = process.errorStream.bufferedReader().readText()
                val output = process.inputStream.bufferedReader().readText()
                
                when {
                    exitCode == 0 -> {
                        if (checkOnly.get()) {
                            if (output.isNotBlank()) {
                                needsFormat.add(file)
                                logger.info("need to format: ${file.relativeTo(project.rootDir)}")
                            }
                        } else {
                            formatted.add(file)
                            logger.info("already formatted: ${file.relativeTo(project.rootDir)}")
                        }
                    }
                    checkOnly.get() && exitCode != 0 -> {
                        failed.add(file to errorOutput)
                        logger.lifecycle("grammr not fit: ${file.relativeTo(project.rootDir)}")
                    }
                    else -> {
                        failed.add(file to errorOutput)
                        logger.error("process error: ${file.relativeTo(project.rootDir)}")
                    }
                }
            } catch (e: Exception) {
                failed.add(file to (e.message ?: "unknown error"))
            }
        }
        
        return ProcessResults(formatted, failed, needsFormat)
    }
    
    private fun printResults(results: ProcessResults) {
        val projectName = project.name
        val isCheck = checkOnly.get()
        
        if (isCheck) {
            when {
                results.failedFiles.isEmpty() && results.needsFormat.isEmpty() -> 
                    logger.lifecycle("$projectName: ✅ All correct")
                results.failedFiles.isNotEmpty() -> {
                    logger.lifecycle("$projectName: ❌ ${results.failedFiles.size} files error")
                results.failedFiles.forEach {
                    logger.error("${it.first.relativeTo(project.rootDir)} - ${it.second}")
                }
                }
                else -> 
                    logger.lifecycle("$projectName: ⚠️  ${results.needsFormat.size} files need to be formatted")
            }
        } else {
            logger.lifecycle("$projectName: finish - ${results.formattedFiles.size} files formated")
            results.formattedFiles.forEach {
                logger.lifecycle("${it.relativeTo(project.rootDir)}")
            }
            if (results.failedFiles.isNotEmpty()) {
                logger.lifecycle("$projectName: fail - ${results.failedFiles.size} files")
                results.failedFiles.forEach {
                    logger.error("${it.first.relativeTo(project.rootDir)} - ${it.second}")
                }
            }
        }
    }
    
    private data class ProcessResults(
        val formattedFiles: List<File>,
        val failedFiles: List<Pair<File, String>>,
        val needsFormat: List<File>
    )
}
