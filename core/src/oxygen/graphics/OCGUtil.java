/* (C) 2025 */
package oxygen.graphics;

import arc.files.*;
import arc.graphics.*;
import mindustry.*;

public class OCGUtil {
  public static Fi getCubeMapT(String path) {
    return Vars.tree.get("cubemaps/" + path);
  }

  public static Cubemap getCubeMap(String base) {
    return new Cubemap(getCubeMapT(base + "right.png"), getCubeMapT(base + "left.png"),
        getCubeMapT(base + "top.png"), getCubeMapT(base + "bottom.png"),
        getCubeMapT(base + "front.png"), getCubeMapT(base + "back.png"));
  }
}
