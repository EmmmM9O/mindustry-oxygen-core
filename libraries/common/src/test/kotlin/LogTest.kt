import org.junit.jupiter.api.*
import oxygen.util.*

class LogTest {
    @Test
    fun defaultLog() {
        logConfig {
        }.logger {
            fun <T : Any> T?.defFormat() = formatOrEmpty { "[$it]" }
            name(LogTest::class)
            formatter {
                "${logger.name.defFormat()}${level.defFormat()}${mark?.name().defFormat()}:$message \n${
                    cause.workOrEmpty {
                        throwableMsg(
                            it
                        )
                    }
                }"
            }
            appender {
                println(it)
            }
        }.apply {
            atInfo {
                mark { "Test" }
                message("Test Log")
            }
        }
    }
    @Test
    fun simpleLog() {
        logConfig { }.logger {
            name(LogTest::class)
            simpleFormat {
                template { "$timeStr$levelColor<${logger.name}>[$level]$message" }
                oxyTime { "<$HH:$mm:$ss>" }
            }
        }.atInfo {
            mark { "Test" }
            message("Simple Logger Test")
        }
    }
}