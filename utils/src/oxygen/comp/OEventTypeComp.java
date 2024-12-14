/* (C) 2024 */
package oxygen.comp;

import static oxygen.annotations.generator.GenType.*;

import oxygen.annotations.generator.*;
import oxygen.loader.*;

/**
 * OEventTypeComp
 */
@AutoGen(value = EventTypeG, path = "oxygen.game")
@ML.EventType
public class OEventTypeComp {
    public enum OTrigger {
        init,
    }
}
