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
    public EventProcessor eventProcessor;

    public EventBus() {
        events = new OEvents();
        processor = new MLProcessor();
        processor.standardProcessors();
        eventProcessor = new EventProcessor(events);
        processor.annotationProcessors.put(ML.Event.class, eventProcessor);
    }

    public void init() {
        processor.init();
    }
}
