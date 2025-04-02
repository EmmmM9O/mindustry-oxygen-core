/* (C) 2025 */
package oxygen.ui;

import arc.math.*;

import arc.scene.style.*;
import mindustry.gen.*;
import oxygen.annotations.generator.*;
import oxygen.core.*;
import oxygen.gen.*;
import oxygen.ui.dialogs.*;
import oxygen.ui.draw.*;

import static oxygen.annotations.generator.GenType.*;
import static oxygen.ui.OCPal.*;
import static oxygen.ui.OUI.*;
import static oxygen.ui.UIDraws.*;

@AutoGen(value = TexSG, path = "oxygen.gen", className = "OCTex", withS2 = OCMain.name + "-")
public class OCStyles {
  public static Drawable oorangeDrawable, oorange3, owhite3, owhite6, oacid3, ocyanDrawable,
      odarkDrawable, odark6;

  public static void load() {
    var whiteui = (TextureRegionDrawable) Tex.whiteui;
    oorangeDrawable = whiteui.tint(oorange);
    ocyanDrawable = whiteui.tint(ocyan);
    oorange3 = whiteui.tint(oorange.cpy().a(0.3f));
    owhite3 = whiteui.tint(owhite.cpy().a(0.3f));
    owhite6 = whiteui.tint(owhite.cpy().a(0.6f));
    oacid3 = whiteui.tint(oacid.cpy().a(0.3f));
    odarkDrawable = whiteui.tint(odark);
    odark6 = whiteui.tint(odark.cpy().a(0.6f));
    OCTex.loadStyles();
  }

  public static final float startDuration = 0.1f, endDuration = 0.05f;
  public static final Interp startInterp = Interp.exp5Out, endInterp = Interp.exp5In;

  public static OButton ocTButton(String text, Runnable func) {
    OButton res = obutton(combineDraw(loadDraw(leftTextCons(text)),
        overTimeDraw(startDuration, endDuration,
            self -> combineDraw(
                slideBackgroundDraw(Direction.right, oacid3,
                    timeProgress(startInterp, endInterp, self)),
                moveSideDraw(10f, 5f, 3f, Direction.right, ocyanDrawable,
                    timeProgress(startInterp, endInterp, self))))));
    res.clicked(func);
    return res;
  }

  public static OButton ocCloseButton(String text, Runnable func) {
    OButton res = obutton(
        combineDraw(loadDraw(textCons(text)),
            overTimeDraw(startDuration, endDuration, self -> slideBackgroundDraw(Direction.right, oacid3,
                timeProgress(startInterp, endInterp, self)))));
    res.clicked(func);
    return res;
  }

  public static UIDraw<OCDialog> ocDialogDraw() {
    return backgroundDraw(odarkDrawable);
  }
}
