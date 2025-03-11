/* (C) 2025 */
package oxygen.ui;

import oxygen.annotations.generator.*;
import oxygen.core.*;
import oxygen.gen.*;

import static oxygen.annotations.generator.GenType.*;

@AutoGen(value = TexSG, path = "oxygen.gen", className = "OCTex", withS2 = OCMain.name + "-")
public class OCStyles {
  public static void load() {
    OCTex.loadStyles();
  }
}
