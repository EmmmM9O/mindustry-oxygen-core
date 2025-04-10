package oxygen.graphics.generator;

import arc.*;
import arc.graphics.Pixmap.*;
import arc.graphics.gl.*;
import arc.struct.*;

public class NNLSSpencerBloomGenerator {
  public static final float[][] downSample = {
      { 1.0f / 81.0f, 3.0f / 81.0f, 3.0f / 81.0f, 1.0f / 81.0f },
      { 3.0f / 81.0f, 9.0f / 81.0f, 9.0f / 81.0f, 3.0f / 81.0f },
      { 3.0f / 81.0f, 9.0f / 81.0f, 9.0f / 81.0f, 3.0f / 81.0f },
      { 1.0f / 81.0f, 3.0f / 81.0f, 3.0f / 81.0f, 1.0f / 81.0f } };

  public static final float[][] upSample = { //
      { 1.0f / 16.0f, 3.0f / 16.0f, 3.0f / 16.0f, 9.0f / 16.0f },
      { 3.0f / 16.0f, 1.0f / 16.0f, 9.0f / 16.0f, 3.0f / 16.0f },
      { 3.0f / 16.0f, 9.0f / 16.0f, 1.0f / 16.0f, 3.0f / 16.0f },
      { 9.0f / 16.0f, 3.0f / 16.0f, 3.0f / 16.0f, 1.0f / 16.0f } };
  public int maxLevel = 9, height, size;

  int getLevel(int h) {
    return h <= 2 ? 0 : getLevel(h / 2 + h % 2) + 1;
  }

  int getLevels(int h) {
    return getLevel(h) < maxLevel ? getLevel(h) : maxLevel;
  }

  public NNLSSpencerBloomGenerator(int level) {
    this.maxLevel = level;
  }

  public double filter(double x, double y) {
    double r = Math.sqrt(x * x + y * y);
    return pow(0.02 / (r / r0 + 0.02), 3.0);
  }

  public void generate(int height, int size) {
    int levels = getLevels(height);
    int border = 2;
    for (int i = 1; i < levels; ++i) {
      border = 2 * (border + 1);
    }
    double integral = 0.0;
    for (int y = -border; y < size + border; ++y) {
      for (int x = -border; x < size + border; ++x) {
        integral += filter(x - size / 2, y - size / 2);
      }
    }
  }
}
