/* (C) 2024 */
package oxygen.utils;

import oxygen.loader.*;

/**
 * EventBus
 */
public class EventBus {
    public MLProcessor processor;

    public EventBus() {
        processor = new MLProcessor();
    }

    public void init() {
        processor.init();
    }
}
