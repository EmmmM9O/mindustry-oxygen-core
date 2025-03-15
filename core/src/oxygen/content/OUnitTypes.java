/* (C) 2025 */
package oxygen.content;

import mindustry.type.*;
import oxygen.entities.units.TestUnit;
import oxygen.type.*;

public class OUnitTypes {
  public static UnitType test;

  public static void load() {
    test = new OUnitType("test") {
      {
        maxHeight = 30f;
        constructor = TestUnit::new;
      }
    };
  }
}
