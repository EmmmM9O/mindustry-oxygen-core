/* (C) 2025 */
package oxygen.graphics.bloom;

import arc.graphics.*;
import oxygen.graphics.OCShaders.*;

public class PyramidFourNAvgBloom extends
    PyramidBloom<BloomBrightness, BloomComposite, BloomUpsample, BloomDownsample, BloomTonemapping> {
  public PyramidFourNAvgBloom(Mesh screen, int width, int height) {
    super(screen, width, height);
  }

  @Override
  BloomBrightness createBrightness() {
    return new BloomBrightness();
  }

  @Override
  BloomComposite createComposite() {
    return new BloomComposite();
  }

  @Override
  BloomUpsample createUpsample() {
    return new BloomUpsample("bloom/fnavg_upsample", "screen");
  }

  @Override
  BloomDownsample createDownsample() {
    return new BloomDownsample("bloom/fnavg_downsample", "screen");
  }

  @Override
  BloomTonemapping createTonemapping() {
    return new BloomTonemapping();
  }
}
