package oxygen.util

import arc.util.*
import kotlin.reflect.*

val ArcLogAppender: Appender = {
    when (level) {
        Level.TRACE -> Log.info("Trace:$it")
        Level.DEBUG -> Log.debug(it)
        Level.INFO -> Log.info(it)
        Level.WARN -> Log.warn(it)
        Level.ERROR -> Log.err(it)
        Level.FATAL -> Log.err("Fatal:$it")
    }
}

fun Level.toLevel() = when (this) {
    Level.TRACE -> Log.LogLevel.none
    Level.DEBUG -> Log.LogLevel.debug
    Level.INFO -> Log.LogLevel.info
    Level.WARN -> Log.LogLevel.warn
    Level.ERROR -> Log.LogLevel.err
    Level.FATAL -> Log.LogLevel.err
}

fun Log.LogLevel.toLevel() = when (this) {
    Log.LogLevel.none -> Level.TRACE
    Log.LogLevel.info -> Level.INFO
    Log.LogLevel.debug -> Level.DEBUG
    Log.LogLevel.warn -> Level.WARN
    Log.LogLevel.err -> Level.ERROR
}

fun LoggerConfigure.arcAppender() {
    appender = ArcLogAppender
}

class ArcLogHandlerAppender : Appender {
    var origin: Log.LogHandler = Log.logger
    override fun invoke(event: LoggingEvent, text: String) {
        origin.log(event.level.toLevel(), if (event.level == Level.FATAL) "FATAL" else "" + text)
    }
}

fun LoggerConfigure.arcHandlerAppender() {
    appender = ArcLogHandlerAppender()
}

object Marks {
    val log = StrMark("Log")
    fun processor(clazz: KClass<*>) = StrMark("Processor ${clazz.simpleName}")
}