/* (C) 2024 */
package oxygen.core;

import static oxygen.utils.StandardEventPriority.*;

import arc.util.Log;
import oxygen.loader.*;

/**
 * Test
 */
public class Test {
    public static boolean flag = false;

    @ML.Event(value = HIGHEST, event = "Trigger")
    public static void update() {
        if (!flag) {
            flag = true;
            Log.info("test event");
        }
    }
}
