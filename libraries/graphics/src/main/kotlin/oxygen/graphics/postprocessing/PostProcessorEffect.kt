package oxygen.graphics.postprocessing

import arc.graphics.gl.*;

abstract class PostProcessorEffect {
    open var enabled = true
    abstract fun renderTo(src: FrameBuffer, dest: FrameBuffer)
}
