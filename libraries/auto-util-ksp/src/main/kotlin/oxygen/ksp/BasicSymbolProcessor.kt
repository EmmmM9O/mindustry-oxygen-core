package oxygen.ksp


import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import oxygen.annotations.*
import oxygen.util.*
import oxygen.util.flow.*
import kotlin.reflect.*

typealias AnnoElements = Map<String, Sequence<KSAnnotated>>

inline fun <reified T : Annotation> AnnoElements.getElements() =
    get(T::class.qualifiedName.toString()) ?: sequenceOf()

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
        steps.forEach { it.finish(this) }
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

data class AnnotatedAnnoData(
    val oriAnno: KSAnnotation,
    val annotated: Sequence<Pair<KSAnnotation, KSAnnotated>>
)

interface Step {
    fun annotations(): Set<String>
    fun process(processor: BasicSymbolProcessor, resolver: Resolver, elements: AnnoElements): List<KSAnnotated>
    fun finish(processor: BasicSymbolProcessor) {}
}

fun stepProcess(environment: SymbolProcessorEnvironment, steps: Set<Step>) =
    object : BasicSymbolProcessor(environment) {
        override val steps: Set<Step>
            get() = steps
    }

// EnhancedStep
interface AnnoSeeker<R> {
    fun annotations(): Set<String>
    fun seek(processor: BasicSymbolProcessor, resolver: Resolver, elements: AnnoElements): R
}

abstract class EnhancedStep<A> : Step {
    lateinit var seeker: AnnoSeeker<A>

    override fun annotations(): Set<String> = seeker.annotations()

    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> {
        if (!this::seeker.isInitialized) {
            processor.error("$this step's Seeker is not initialized")
            return emptyList()
        }
        return actualProcess(processor, resolver, seeker.seek(processor, resolver, elements))
    }

    abstract fun actualProcess(processor: BasicSymbolProcessor, resolver: Resolver, args: A): List<KSAnnotated>
}

open class BaseWorkspace(val processor: BasicSymbolProcessor) {
    fun info(str: String) {
        processor.info(str)
    }

    fun debug(str: String) {
        processor.debug(str)
    }

    fun error(err: String) {
        processor.error(err)
    }
}

class ProcessWorkspace(processor: BasicSymbolProcessor, val resolver: Resolver, val result: MutableList<KSAnnotated>) :
    BaseWorkspace(processor) {
    fun defer(annotated: KSAnnotated) {
        result.add(annotated)
    }

    fun deferAll(vararg annotateds: KSAnnotated) {
        result.addAll(annotateds)
    }
}

class FuncEnhancedStep<A>(
    seeker: AnnoSeeker<A>,
    var processFunc: ProcessWorkspace.(arg: A) -> Unit,
    var finishFunc: (BaseWorkspace.() -> Unit)? = null
) : EnhancedStep<A>() {
    init {
        this.seeker = seeker
    }

    var result: MutableList<KSAnnotated> = mutableListOf()

    override fun actualProcess(processor: BasicSymbolProcessor, resolver: Resolver, args: A): List<KSAnnotated> {
        result = mutableListOf()
        ProcessWorkspace(processor, resolver, result).processFunc(args)
        return result
    }

    override fun finish(processor: BasicSymbolProcessor) {
        finishFunc?.invoke(BaseWorkspace(processor))
    }

    inner class Handler {
        fun finish(finishF: BaseWorkspace.() -> Unit) {
            finishFunc = finishF
        }
    }

    fun createHandler() = Handler()
}

class SimpleSeeker(vararg annotations: String) : AnnoSeeker<AnnoElements> {
    val annos: Set<String> = setOf(*annotations)
    override fun annotations(): Set<String> = annos
    override fun seek(processor: BasicSymbolProcessor, resolver: Resolver, elements: AnnoElements): AnnoElements =
        elements
}

class IndividualSeeker<T : Annotation>(target: KClass<T>) : AnnoSeeker<Sequence<KSAnnotated>> {
    val target: String = target.qualifiedName!!
    override fun annotations(): Set<String> = setOf(target)
    override fun seek(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): Sequence<KSAnnotated> = elements.values.first()
}

class IndividualEachSeeker<T : Annotation>(target: KClass<T>, val resolver: (anno: KSAnnotation) -> T) :
    AnnoSeeker<Sequence<Pair<KSAnnotated, T>>> {
    val target: String = target.qualifiedName!!
    override fun annotations(): Set<String> = setOf(target)
    override fun seek(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): Sequence<Pair<KSAnnotated, T>> =
        elements.values.first().map { element ->
            element to resolver(element.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == target }!!)
        }
}

class SubclassSeeker(val target: String) : AnnoSeeker<Sequence<Pair<KSAnnotation, KSAnnotated>>> {
    override fun annotations(): Set<String> = setOf(target)
    override fun seek(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): Sequence<Pair<KSAnnotation, KSAnnotated>> =
        elements.values.first().map { it.annotations.first { a -> a.isInheritedFrom(target) } to it }
}

class AnnotatedAnnotationSeeker(val target: String) : AnnoSeeker<Sequence<AnnotatedAnnoData>> {
    override fun annotations(): Set<String> = setOf(target)
    override fun seek(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): Sequence<AnnotatedAnnoData> =
        elements.values.first().map { anno ->
            AnnotatedAnnoData(
                anno.getAnnotationByName(target)!!,
                run {
                    val name = (anno as KSClassDeclaration).qualifiedName!!.asString()
                    resolver.getSymbolsWithAnnotation(name).map {
                        it.getAnnotationByName(name)!! to it
                    }
                })
        }
}
typealias EPFunc<A> = ProcessWorkspace.(arg: A) -> Unit
typealias EPHandler<A> = FuncEnhancedStep<A>.Handler

@Dsl
class EnhancedStepsMarker {
    val set = mutableSetOf<Step>()

    fun <T : Step> addSelf(step: T): T {
        set.add(step)
        return step
    }

    fun add(step: Step) {
        set.add(step)
    }

    fun <A> process(seeker: AnnoSeeker<A>, func: EPFunc<A>)
            : EPHandler<A> = addSelf(FuncEnhancedStep(seeker, func)).createHandler()

    inline fun <reified T : Annotation> individual(noinline func: EPFunc<Sequence<KSAnnotated>>)
            : EPHandler<Sequence<KSAnnotated>> = process(IndividualSeeker(T::class), func)

    inline fun <reified T : Annotation> individualEach(
        noinline resolver: (anno: KSAnnotation) -> T,
        noinline func: EPFunc<Sequence<Pair<KSAnnotated, T>>>
    )
            : EPHandler<Sequence<Pair<KSAnnotated, T>>> = process(IndividualEachSeeker(T::class, resolver), func)

    inline fun <reified T : Annotation> subclass(noinline func: EPFunc<Sequence<Pair<KSAnnotation, KSAnnotated>>>)
            : EPHandler<Sequence<Pair<KSAnnotation, KSAnnotated>>> =
        process(SubclassSeeker(T::class.qualifiedName!!), func)

    inline fun <reified T : Annotation> annotatedAnno(noinline func: EPFunc<Sequence<AnnotatedAnnoData>>)
            : EPHandler<Sequence<AnnotatedAnnoData>> =
        process(AnnotatedAnnotationSeeker(T::class.qualifiedName!!), func)

    fun build(): Set<Step> {
        return set
    }
}

fun processESteps(block: EnhancedStepsMarker.() -> Unit): Set<Step> = EnhancedStepsMarker().apply(block).build()
fun stepEProcess(environment: SymbolProcessorEnvironment, block: EnhancedStepsMarker.() -> Unit) =
    stepProcess(environment, processESteps(block))

//Chain Step Processor
class ChainStep(val seeker: AnnoSeeker<*>) : Step {
    lateinit var processFunc: ProcessWorkspace.(elements: AnnoElements) -> Unit
    lateinit var finishFunc: BaseWorkspace.() -> Unit
    var result: MutableList<KSAnnotated> = mutableListOf()

    override fun annotations(): Set<String> = seeker.annotations()

    override fun process(
        processor: BasicSymbolProcessor,
        resolver: Resolver,
        elements: AnnoElements
    ): List<KSAnnotated> {
        if (!this::processFunc.isInitialized) {
            processor.error("$this step's process function is not initialized")
            return emptyList()
        }
        result = mutableListOf()
        ProcessWorkspace(processor, resolver, result).processFunc(elements)
        return result
    }

    override fun finish(processor: BasicSymbolProcessor) {
        if (this::finishFunc.isInitialized) {
            BaseWorkspace(processor).finishFunc()
        }
    }

    fun createHandler() = ChainStepHandler(this)
}

class ChainStepHandler(val step: ChainStep) {
    fun finish(finishF: BaseWorkspace.() -> Unit) {
        step.finishFunc = finishF
    }
}

data class SeekerData(val processor: BasicSymbolProcessor, val resolver: Resolver, val elements: AnnoElements)
typealias SeekerChain<A> = ChainOperation<ProcessWorkspace, SeekerData, A>

@Dsl
class ChainStepsMarker {
    val set = mutableSetOf<Step>()
    var tmp: AnnoSeeker<*>? = null

    fun <T : Step> add(step: T): T {
        set.add(step)
        return step
    }

    fun <A> seek(seeker: AnnoSeeker<A>): ChainOperation<ProcessWorkspace, SeekerData, A> = seeker.let {
        tmp = seeker
        ChainSimple { seeker.seek(it.processor, it.resolver, it.elements) }
    }

    inline fun <reified T : Annotation> individual(): ChainSeq<ProcessWorkspace, SeekerData, KSAnnotated> =
        IndividualSeeker(T::class).let { s ->
            tmp = s
            ChainSeq { s.seek(it.processor, it.resolver, it.elements) }
        }

    inline fun <reified T : Annotation> individualEach(noinline resolver: (anno: KSAnnotation) -> T): ChainSeq<ProcessWorkspace, SeekerData, Pair<KSAnnotated, T>> =
        IndividualEachSeeker(T::class, resolver).let { s ->
            tmp = s
            ChainSeq { s.seek(it.processor, it.resolver, it.elements) }
        }

    inline fun <reified T : Annotation> subclass(): ChainSeq<ProcessWorkspace, SeekerData, Pair<KSAnnotation, KSAnnotated>> =
        SubclassSeeker(T::class.qualifiedName!!).let { s ->
            tmp = s
            ChainSeq { s.seek(it.processor, it.resolver, it.elements) }
        }

    inline fun <reified T : Annotation> annotatedAnno(): ChainSeq<ProcessWorkspace, SeekerData, AnnotatedAnnoData> =
        AnnotatedAnnotationSeeker(T::class.qualifiedName!!).let { s ->
            tmp = s
            ChainSeq { s.seek(it.processor, it.resolver, it.elements) }
        }

    fun <N> ChainOperation<ProcessWorkspace, SeekerData, N>.process(func: ProcessWorkspace.(arg: N) -> Unit)
            : ChainStepHandler {
        if (tmp == null) throw IllegalArgumentException("Seeker must be initialized")
        val loc = tmp!!
        return add(ChainStep(loc)).let {
            tmp = null
            it.processFunc = { elements ->
                func(invoke(this, SeekerData(processor, resolver, elements)))
            }
            it.createHandler()
        }
    }

    fun finish(func: BaseWorkspace.() -> Unit) {
        val last = set.last()
        if (last is ChainStep)
            last.finishFunc = func
    }

    fun <N> ChainOperation<ProcessWorkspace, SeekerData, Sequence<N>>.processEach(func: ProcessWorkspace.(arg: N) -> Unit)
            : ChainStepHandler = process { seq -> seq.forEach { func(it) } }

    fun build(): Set<Step> {
        return set
    }
}

fun processChain(block: ChainStepsMarker.() -> Unit): Set<Step> = ChainStepsMarker().apply(block).build()
fun chainProcess(environment: SymbolProcessorEnvironment, block: ChainStepsMarker.() -> Unit) =
    stepProcess(environment, processChain(block))
