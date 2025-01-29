/* (C) 2024 */
package oxygen.utils;

import oxygen.loader.*;

/**
 * EventBus
 */
public class EventBus {

  public MLProcessor processor;
  public OEvent events;

  public EventBus(OEvent events) {
    this.events = events;
    processor = new MLProcessor();
    AnnotationProcessors.setStandardProcessor(processor);
    AnnotationProcessors.setEventProcessor(processor, events);
  }

  public void init() {
    processor.init();
  }
}
