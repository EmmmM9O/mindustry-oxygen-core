/* (C) 2024 */
package oxygen.utils;

import oxygen.loader.*;
import oxygen.loader.MLProcessor.*;

/**
 * EventBus
 */
public class EventBus {

    public MLProcessor processor;
    public OEvents events;

    public EventBus() {
        events = new OEvents();
        processor = new MLProcessor();
        AnnotationProcessors.setStandardProcessor(processor);
    }

    public void init() {
        processor.init();
    }
}
