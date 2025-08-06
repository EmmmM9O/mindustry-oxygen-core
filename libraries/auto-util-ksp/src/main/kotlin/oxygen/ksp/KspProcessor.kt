package oxygen.ksp

import oxygen.annotations.*
import oxygen.util.*

class ProcessorImpl(val processor: BasicSymbolProcessor) : Processor {
    override val logger: OLogger
        get() = processor.logger

    override fun info(str: String) {
        processor.info(str)
    }

    override fun error(str: String) {
        processor.error(str)
    }

    override fun debug(str: String) {
        processor.debug(str)
    }
}