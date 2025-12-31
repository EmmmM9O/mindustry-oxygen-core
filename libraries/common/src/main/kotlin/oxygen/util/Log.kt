package oxygen.util

import kotlinx.datetime.*
import oxygen.annotations.*
import kotlin.reflect.*

object LevelInts {
    const val TRACE: Int = 0
    const val DEBUG: Int = 10
    const val INFO: Int = 20
    const val WARN: Int = 30
    const val ERROR: Int = 40
    const val FATAL: Int = 50
    fun toLevel(level: Int): Level {
        return when (level) {
            TRACE -> Level.TRACE
            DEBUG -> Level.DEBUG
            INFO -> Level.INFO
            WARN -> Level.WARN
            ERROR -> Level.ERROR
            FATAL -> Level.FATAL
            else -> throw IllegalArgumentException("level $level unfound")
        }
    }
}

enum class Level(val level: Int, val text: String) {
    TRACE(LevelInts.TRACE, "Trace"), DEBUG(LevelInts.DEBUG, "Debug"), INFO(LevelInts.INFO, "Info"), WARN(
        LevelInts.WARN,
        "Warn"
    ),
    ERROR(LevelInts.ERROR, "Error"), FATAL(LevelInts.FATAL, "Fatal");

    fun toInt(): Int {
        return level
    }

    override fun toString(): String {
        return text
    }
}

fun interface Mark {
    fun name(): String
}

data class StrMark(val name: String) : Mark {
    override fun name(): String = name
}

open class LoggingEventBuilder(
    var level: Level = Level.TRACE,
    var mark: Mark? = null,
    var message: String? = null,
    var cause: Throwable? = null,
    var data: Any? = null
) {
    constructor(builder: LoggingEventBuilder) : this(builder.level, builder.mark, builder.message, builder.cause)
}

@Dsl
class LoggingEventBuilderDsl : LoggingEventBuilder() {
    fun level(level: Level) {
        this.level = level
    }

    fun mark(mark: Mark) {
        this.mark = mark
    }

    fun message(message: String) {
        this.message = message
    }

    fun cause(cause: Throwable?) {
        this.cause = cause
    }

    fun data(data: Any?) {
        this.data = data
    }

    fun build(): LoggingEventBuilder {
        require(message != null) { "Must set message" }
        return this
    }
}

open class LoggingEvent(
    level: Level,
    mark: Mark?,
    message: String?,
    cause: Throwable?,
    data: Any?,
    var logger: OLogger,
    var time: LocalDateTime,
    var location: SourceLocation
) :
    LoggingEventBuilder(level, mark, message, cause, data) {
    constructor(
        builder: LoggingEventBuilder,
        logger: OLogger,
        time: LocalDateTime,
        location: SourceLocation
    ) : this(
        builder.level, builder.mark, builder.message, builder.cause, builder.cause, logger, time, location
    )

    constructor(event: LoggingEvent)
            : this(
        event.level,
        event.mark,
        event.message,
        event.cause,
        event.data,
        event.logger,
        event.time,
        event.location
    )
}

fun LoggingEventBuilderDsl.mark(mark: String) {
    this.mark = StrMark(mark)
}

interface ErrorMessager {
    fun getErrorLog(e: Exception): String
}


object DefaultErrorMessager : ErrorMessager {
    override fun getErrorLog(e: Exception): String = "failed $e"
}

typealias LogFormatter = LoggingEvent.() -> String

val DefaultFormatter: LogFormatter = {
    fun <T : Any> T?.defFormat() = formatOrEmpty { "[$it]" }
    val timeFormat = LocalDateTime.Format {
        chars("T")
        hour()
        chars(":")
        minute()
        chars(":")
        second()
    }
    "${
        time.format(timeFormat).defFormat()
    }${
        mark?.name().defFormat()
    }${logger.name.defFormat()}[$level]:$message \n${cause.workOrEmpty { ":${throwableMsg(it)}" }}"
}
typealias Appender = LoggingEvent.(text: String) -> Unit

val NoopAppender: Appender = {}
val DefaultAppender: Appender = {
    fun Level.color(): String = when (this) {
        Level.TRACE -> "\u001B[2m"
        Level.DEBUG -> "\u001B[3m"
        Level.INFO -> "\u001B[34;1m"
        Level.WARN -> "\u001B[33;1m"
        Level.ERROR -> "\u001B[31;1m"
        else -> ""
    }
    println("${level.color()}$it\u001B[0m")
}


open class LoggerConfigure(
    var errorMessager: ErrorMessager = DefaultErrorMessager,
    var logLevel: Level = Level.INFO,
    var formatter: LogFormatter = DefaultFormatter,
    var appender: Appender = DefaultAppender
)

interface OLogger {
    val name: String?
    val config: LoggerConfigure
    fun (() -> Any?).toStringSafe(): String = try {
        invoke().toString()
    } catch (e: Exception) {
        config.errorMessager.getErrorLog(e)
    }

    fun at(block: LoggingEventBuilderDsl.() -> Unit) = at(LoggingEventBuilderDsl().apply(block).build())

    fun at(event: LoggingEventBuilder) =
        event.level.takeIf { it.isEnabled() }
            ?.let { LoggingEvent(event, this, timeNow(), SourceLocation()) }
            ?.let { with(config) { appender(it, formatter(it)) } } ?: Unit

    fun trace(text: () -> Any?): Unit = at {
        level(Level.TRACE)
        message(text.toStringSafe())
    }

    fun debug(text: () -> Any?): Unit = at {
        level(Level.DEBUG)
        message(text.toStringSafe())
    }

    fun info(text: () -> Any?): Unit = at {
        level(Level.INFO)
        message(text.toStringSafe())
    }

    fun warn(text: () -> Any?): Unit = at {
        level(Level.WARN)
        message(text.toStringSafe())
    }

    fun error(text: () -> Any?): Unit = at {
        level(Level.ERROR)
        message(text.toStringSafe())
    }

    fun fatal(text: () -> Any?): Unit = at {
        level(Level.FATAL)
        message(text.toStringSafe())
    }

    fun error(throwable: Throwable?, text: () -> Any?): Unit = at {
        level(Level.ERROR)
        cause(throwable)
        message(text.toStringSafe())
    }

    fun fatal(throwable: Throwable?, text: () -> Any?): Unit = at {
        level(Level.FATAL)
        cause(throwable)
        message(text.toStringSafe())
    }

    fun atTrace(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.TRACE)
        block()
    }

    fun atDebug(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.DEBUG)
        block()
    }

    fun atInfo(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.INFO)
        block()
    }

    fun atWarn(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.DEBUG)
        block()
    }

    fun atError(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.ERROR)
        block()
    }

    fun atFatal(block: LoggingEventBuilderDsl.() -> Unit): Unit = at {
        level(Level.FATAL)
        block()
    }

    fun entry(vararg arguments: Any?): Unit = trace { "entry(${arguments.joinToString()})" }
    fun Level.isEnabled(): Boolean = this.ordinal >= config.logLevel.ordinal
    fun loggingEnabled(level: Level, mark: Mark? = null): Boolean = level.isEnabled()
    fun traceEnabled(mark: Mark? = null): Boolean = loggingEnabled(Level.TRACE, mark)
    fun debugEnabled(mark: Mark? = null): Boolean = loggingEnabled(Level.DEBUG, mark)
    fun infoEnabled(mark: Mark? = null): Boolean = loggingEnabled(Level.INFO, mark)
    fun warnEnabled(mark: Mark? = null): Boolean = loggingEnabled(Level.WARN, mark)
    fun errorEnabled(mark: Mark? = null): Boolean = loggingEnabled(Level.ERROR, mark)
}

class DefaultLogger(
    override val name: String?,
    override val config: LoggerConfigure
) : OLogger

interface NameResolver {
    fun name(func: () -> Unit): String
    fun name(clazz: KClass<*>): String
}

@Dsl
class NameResolverDsl : NameResolver {
    lateinit var nameFunc: (func: () -> Unit) -> String
    lateinit var nameClass: (clazz: KClass<*>) -> String
    override fun name(func: () -> Unit): String = nameFunc(func)
    override fun name(clazz: KClass<*>): String = nameClass(clazz)
    fun nameF(nameFunc: (func: () -> Unit) -> String) {
        this.nameFunc = nameFunc
    }

    fun nameC(nameClass: (clazz: KClass<*>) -> String) {
        this.nameClass = nameClass
    }

    fun build(): NameResolver {
        require(this::nameFunc.isInitialized) { "Must set nameFunc" }
        require(this::nameClass.isInitialized) { "Must set nameClass" }
        return this
    }
}


object DefaultNameResolver : NameResolver {
    override fun name(func: () -> Unit): String = when (func) {
        is KFunction<*> -> func.name
        else -> func::class.simpleName.toString()
    }

    override fun name(clazz: KClass<*>): String = clazz.java.name.toCleanClassName()
    private val classNameEndings = listOf("Kt$", "$")
    private fun String.toCleanClassName(): String {
        classNameEndings.forEach { ending ->
            val indexOfEnding = this.indexOf(ending)
            if (indexOfEnding != -1) {
                return this.substring(0, indexOfEnding)
            }
        }
        return this
    }
}

interface OLoggerFactory {
    fun logger(name: String?, configure: LoggerConfigure): OLogger
}

object DefaultLoggerFactory : OLoggerFactory {
    override fun logger(name: String?, configure: LoggerConfigure): OLogger = DefaultLogger(name, configure)
}

open class LoggerConfigureDslBase : LoggerConfigure() {
    fun errorMessager(errorMessager: ErrorMessager) {
        this.errorMessager = errorMessager
    }

    fun logLevel(logLevel: Level) {
        this.logLevel = logLevel
    }

    fun formatter(formatter: LogFormatter) {
        this.formatter = formatter
    }

    fun appender(appender: Appender) {
        this.appender = appender
    }

    fun build(): LoggerConfigure {
        return this
    }
}

object OxygenLog {
    @Dsl
    class LoggerConfigureDsl : LoggerConfigureDslBase() {
        var name: String? = null
        fun name(name: String?) {
            this.name = name
        }

        fun name(func: () -> Unit) {
            this.name = nameResolver.name(func)
        }

        fun name(clazz: KClass<*>) {
            this.name = nameResolver.name(clazz)
        }
    }

    var factory: OLoggerFactory? = DefaultLoggerFactory
    var nameResolver: NameResolver = DefaultNameResolver
    fun logger(block: LoggerConfigureDsl.() -> Unit): OLogger =
        LoggerConfigureDsl().apply(block).let {
            factory!!.logger(it.name, it.build())
        }
}

@Dsl
class LogFactoryConfigDsl {
    fun factory(factory: OLoggerFactory) =
        factory.let { OxygenLog.factory = factory }

    fun namer(block: NameResolverDsl.() -> Unit): Unit =
        NameResolverDsl().apply(block).build().let { OxygenLog.nameResolver = it }

    fun check(): LogFactoryConfigDsl {
        require(OxygenLog.factory != null) { "factory can be null" }
        return this
    }
}

fun logConfig(block: LogFactoryConfigDsl.() -> Unit) = LogFactoryConfigDsl().apply(block)
    .let { OxygenLog }

fun LoggerConfigure.defaultError() {
    this.errorMessager = DefaultErrorMessager
}

fun LoggerConfigure.defaultFormatter() {
    this.formatter = DefaultFormatter
}

fun LoggerConfigure.noopAppender() {
    this.appender = NoopAppender
}

fun LoggerConfigure.defaultAppender() {
    this.appender = DefaultAppender
}

@Dsl
class SimpleFormatter : LogFormatter {
    class ILoggingEvent(event: LoggingEvent, val self: SimpleFormatter) :
        LoggingEvent(event) {
        val timeStr: String by lazy { self.timeFormatter(time) }
        val levelColor: String by lazy { self.levelColorF(level) }
    }

    lateinit var templateF: ILoggingEvent.() -> String
    lateinit var timeFormatter: TimeFormatter
    lateinit var levelColorF: Level.() -> String
    fun template(template: ILoggingEvent.() -> String) {
        this.templateF = template
    }

    fun oxyTime(formatter: TimeDataFormatter) {
        this.timeFormatter = timeOxyFormat(formatter)
    }

    fun time(formatter: XTimeFormatter) {
        this.timeFormatter = timeFormat(formatter)
    }

    fun levelColor(color: Level.() -> String) {
        this.levelColorF = color
    }

    fun build(): LogFormatter {
        require(this::templateF.isInitialized) { "must set template" }
        if (!this::timeFormatter.isInitialized)
            timeFormatter = timeFormat {
                chars("T")
                hour()
                chars(":")
                minute()
                chars(":")
                second()
            }
        if (!this::levelColorF.isInitialized)
            this.levelColorF = {
                when (this) {
                    Level.TRACE -> "\u001B[2m"
                    Level.DEBUG -> "\u001B[3m"
                    Level.INFO -> "\u001B[34;1m"
                    Level.WARN -> "\u001B[33;1m"
                    Level.ERROR -> "\u001B[31;1m"
                    else -> ""
                }
            }
        return this
    }

    override fun invoke(event: LoggingEvent): String = templateF(ILoggingEvent(event, this))
}

fun LoggerConfigure.simpleFormat(block: SimpleFormatter.() -> Unit) {
    this.formatter = SimpleFormatter().apply(block).build()
}
