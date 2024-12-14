/* (C) 2024 */
package oxygen.comp;

import static oxygen.annotations.component.ComponentType.*;

import mindustry.world.*;
import oxygen.annotations.component.*;

/**
 * OTilesComp
 */
@AutoComponent(
        value = {Abstract},
        path = "oxygen.world",
        configure = "resource:compConfigure.json:OTilesComp")
public abstract class OTilesComp extends Tiles {

    public OTilesComp() {
        super(0, 0);
    }
}
