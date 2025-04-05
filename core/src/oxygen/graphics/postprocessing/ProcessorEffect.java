package oxygen.graphics.postprocessing;

import arc.graphics.gl.*;
import oxygen.util.*;

public interface ProcessorEffect extends Enableable{
  public void renderTo(FrameBuffer src, FrameBuffer dest);
}
