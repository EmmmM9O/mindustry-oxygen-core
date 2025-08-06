package oxygen.annotations

import oxygen.util.*

interface Processor {
    val logger: OLogger
    fun info(str: String)
    fun debug(str: String)
    fun error(str: String)
}