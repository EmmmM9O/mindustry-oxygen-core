/* (C) 2025 */
package oxygen.graphics.universe;

import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import oxygen.universe.*;

public class UniverseParams {
  public Camera3D cam;
  public float zoom = 1f;
  public SpaceEntity focus;
  public FrameBuffer ray, buffer;
  public Mesh screen;
}
