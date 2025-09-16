package oxygen.ksp


import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*
import oxygen.util.*
import kotlin.reflect.*

abstract class BasicSymbolProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    fun createLogger(): OLogger = logConfig {}.logger {
        name(this::class)
        logLevel(if (debug) Level.DEBUG else Level.INFO)
        simpleFormat {
            fun <T : Any> T?.defFormat() = formatOrEmpty { "[$it]" }
            oxyTime { "<$mm-$ss>" }
            template {
                "$timeStr${
                    mark?.name().defFormat()
                }$levelColor[$level]${logger.name.defFormat()}:$message\n${cause.workOrEmpty { ":${throwableMsg(it)}" }}"
            }
        }
    }

    val logger: OLogger = createLogger()
    val debug: Boolean = environment.options[KConfig.debug]?.toBoolean() ?: false
    abstract val steps: Set<Step>
    lateinit var root: String
    private val deferredElements = mutableMapOf<Step, AnnoElements>()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allDeferred = mutableListOf<KSAnnotated>()
        if (!this::root.isInitialized) {
            if (resolver.getAllFiles().none()) {
                return emptyList()
            }
            initRootDir(resolver)
        }
        steps.forEach { step ->
            val annotations = step.annotations()
            val stepElements = annotations.associateWith { resolver.getSymbolsWithAnnotation(it) }
                .toMutableMap()
            .apply {
                deferredElements[step]?.forEach { (name, elements) ->
                    merge(
                        name,
                        elements
                    ) { old, new -> old + new }
                }
            }
            if (stepElements.isEmpty()) {
                deferredElements.remove(step)
            } else {
                val res = step.process(this, resolver, stepElements)
                if (res.isEmpty())
                    deferredElements.remove(step)
                else {
                    deferredElements[step] = annotations.associateWith { anno ->
                        res.filter { element -> element.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == anno } }
                            .asSequence()
                    }
                    allDeferred.addAll(res)
                }
            }
        }
        return allDeferred
    }

    override fun finish() {
        if (deferredElements.isNotEmpty()) {
            error("Some elements were not processed")
            deferredElements.values.forEach { map ->
                map.values.forEach {
                    error("unprocessed $it")
                }
            }
        }
        steps.forEach{it.finish(this)}
    }

    fun initRootDir(resolver: Resolver) {
        root = environment.options[KConfig.root] ?: run {
            val files = resolver.getAllFiles()
            files.first().filePath.substringBefore("/src/")
        }
        debug("Work at:${root}")
    }

    fun info(str: String) {
        logger.info { str }
        environment.logger.info("[Oxy Info]$str")
    }

    fun debug(str: String) {
        logger.debug { str }
        if (debug)
            environment.logger.info("[Oxy Debug]$str")
    }

    fun error(str: String) {
        logger.error { str }
        environment.logger.error("[Oxy Debug]$str")
    }
}

interface Step {
    fun annotations(): Set<String>
    fun process(processor: BasicSymbolProcessor, resolver: Resolver, elements: AnnoElements): List<KSAnnotated>
    fun finish(processor: BasicSymbolProcessor){}
}

inline fun <reified T : Annotation> AnnoElements.getElements() =
    get(T::class.qualifiedName.toString()) ?: sequenceOf()

abstract class IndividualStep<T : Annotation> : Step {
    var target: String

    constructor(target: KClass<T>) {
        this.target = target.qualifiedName!!
    }

    override fun annotations(): Set<String> {
        return setOf(target)
    }

    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> =
        actualProcess(processor, resolver, elements.values.first())

    abstract fun actualProcess(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: Sequence<KSAnnotated>
    ): List<KSAnnotated>
}

abstract class IndividualStepEach<T : Annotation> : Step {
    var target: String
    var resolver: (element: KSAnnotation) -> T

    constructor(target: KClass<T>, resolver: (element: KSAnnotation) -> T) {
        this.target = target.qualifiedName!!
        this.resolver = resolver
    }

    override fun annotations(): Set<String> {
        return setOf(target)
    }

    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> =
        actualProcess(
            processor, resolver,
            elements.values.first().map { element ->
                element to resolver(element.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == target }!!)
            }
        )

    abstract fun actualProcess(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: Sequence<Pair<KSAnnotated, T>>
    ): List<KSAnnotated>
}

abstract class SubclassStep(val target: String) : Step {
    override fun annotations(): Set<String> = setOf(target)
    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> =
        actualProcess(
            processor,
            resolver,
            elements.values.first().map { it.annotations.first { a -> a.isInheritedFrom(target) } to it }
        )

    abstract fun actualProcess(
        processor: BasicSymbolProcessor, resolver: Resolver,
        elements: Sequence<Pair<KSAnnotation, KSAnnotated>>
    ): List<KSAnnotated>
}


abstract class AnnotatedAnnotationStep(val target: String) : Step {
    data class AnnotatedAnnoData(
        val oriAnno: KSAnnotation,
        val annotated: Sequence<Pair<KSAnnotation, KSAnnotated>>
    )

    override fun annotations(): Set<String> = setOf(target)
    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> =
        actualProcess(
            processor, resolver,
            elements.values.first().map { anno ->
                AnnotatedAnnoData(
                    anno.getAnnotationByName(target)!!,
                    run {
                        val name = (anno as KSClassDeclaration).qualifiedName!!.asString()
                        resolver.getSymbolsWithAnnotation(name).map {
                            it.getAnnotationByName(name)!! to it
                        }
                    })
            })
    /*
    actualProcess(processor, resolver, elements.values.first().map { element ->
    element.annotations.map { anno ->
        processor.info("found ${anno.annotationType}")
        anno.getAnnotationByName(target)?.let {
            anno to it
        }
       }.filterNotNull().takeIf { it.any() }?.let {
        AnnotatedAnnoData(element, it)
    }
}.filterNotNull())*/

    abstract fun actualProcess(
        processor: BasicSymbolProcessor, resolver: Resolver,
        elements: Sequence<AnnotatedAnnoData>
    ): List<KSAnnotated>
}
typealias AnnoElements = Map<String, Sequence<KSAnnotated>>

@Dsl
class StepsMarker {
    val set = mutableSetOf<Step>()
    fun add(step: Step) {
        set.add(step)
    }

    fun process(
        arr: Set<String>,
        func: (processor: BasicSymbolProcessor, resolver: Resolver, elements: AnnoElements) -> List<KSAnnotated>
    ) {
        set.add(object : Step {
            override fun annotations(): Set<String> = arr

            override fun process(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: AnnoElements
            ): List<KSAnnotated> = func(processor, resolver, elements)
        })
    }

    inline fun <reified T : Annotation> individual(crossinline func: (processor: BasicSymbolProcessor, resolver: Resolver, elements: Sequence<KSAnnotated>) -> List<KSAnnotated>) {
        set.add(object : IndividualStep<T>(T::class) {
            override fun actualProcess(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: Sequence<KSAnnotated>
            ): List<KSAnnotated> =
                func(processor, resolver, elements)
        })
    }

    inline fun <reified T : Annotation> individualEach(
        resolver: (anno: KSAnnotation) -> T,
        crossinline func: (processor: BasicSymbolProcessor, resolver: Resolver, elements: Sequence<Pair<KSAnnotated, T>>) -> List<KSAnnotated>
    ) {
        set.add(object : IndividualStepEach<T>(T::class, resolver) {
            override fun actualProcess(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: Sequence<Pair<KSAnnotated, T>>
            ): List<KSAnnotated> = func(processor, resolver, elements)
        })
    }

    inline fun <reified T : Annotation> subclass(
        crossinline func: (
            processor: BasicSymbolProcessor, resolver: Resolver,
            elements: Sequence<Pair<KSAnnotation, KSAnnotated>>
        ) -> List<KSAnnotated>
    ) {
        set.add(object : SubclassStep(T::class.qualifiedName!!) {
            override fun actualProcess(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: Sequence<Pair<KSAnnotation, KSAnnotated>>
            ): List<KSAnnotated> = func(processor, resolver, elements)
        })
    }

    inline fun <reified T : Annotation> annotatedAnno(
        crossinline func: (
            processor: BasicSymbolProcessor, resolver: Resolver,
            elements: Sequence<AnnotatedAnnotationStep.AnnotatedAnnoData>
        ) -> List<KSAnnotated>
    ) {
        set.add(object : AnnotatedAnnotationStep(T::class.qualifiedName!!) {
            override fun actualProcess(
                processor: BasicSymbolProcessor,
                resolver: Resolver,
                elements: Sequence<AnnotatedAnnoData>
            ) = func(processor, resolver, elements)
        })
    }

    fun build(): Set<Step> {
        return set
    }
}

fun processSteps(block: StepsMarker.() -> Unit): Set<Step> = StepsMarker().apply(block).build()

fun stepProcess(environment: SymbolProcessorEnvironment, steps: Set<Step>) =
    object : BasicSymbolProcessor(environment) {
        override val steps: Set<Step>
            get() = steps
    }

fun stepProcess(environment: SymbolProcessorEnvironment, block: StepsMarker.() -> Unit) =
    stepProcess(environment, processSteps(block))
