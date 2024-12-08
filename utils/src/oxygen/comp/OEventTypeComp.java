/* (C) 2024 */
package oxygen.comp;

import static oxygen.annotations.GenType.*;

import oxygen.annotations.*;
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
