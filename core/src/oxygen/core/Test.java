/* (C) 2024 */
package oxygen.core;

import static oxygen.utils.StandardEventPriority.*;

import oxygen.loader.*;

/**
 * Test
 */
public class Test {
    @ML.Event(value = LOW, event = "Oxygen")
    public static void init() {}

    @ML.Event(value = HIGHEST, event = "Trigger")
    public static void update() {}
}
