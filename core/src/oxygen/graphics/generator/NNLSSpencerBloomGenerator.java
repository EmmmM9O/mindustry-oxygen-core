/* (C) 2025 */
package oxygen.graphics.generator;

import arc.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.gl.*;
import arc.struct.*;
import oxygen.graphics.*;
import oxygen.graphics.gl.*;
import oxygen.math.geom.*;

import static oxygen.graphics.OCShaders.*;

public class NNLSSpencerBloomGenerator {

  public static final float[][] UPSAMPLE = { //
      {1.0f / 16.0f, 3.0f / 16.0f, 3.0f / 16.0f, 9.0f / 16.0f},
      {3.0f / 16.0f, 1.0f / 16.0f, 9.0f / 16.0f, 3.0f / 16.0f},
      {3.0f / 16.0f, 9.0f / 16.0f, 1.0f / 16.0f, 3.0f / 16.0f},
      {9.0f / 16.0f, 3.0f / 16.0f, 3.0f / 16.0f, 1.0f / 16.0f}};
  public int maxLevel = 9;
  public float scl = 20;

  int getLevel(int h) {
    return h <= 2 ? 0 : getLevel(h / 2 + h % 2) + 1;
  }

  int getLevels(int h) {
    return getLevel(h) < maxLevel ? getLevel(h) : maxLevel;
  }

  public NNLSSpencerBloomGenerator(int level) {
    this.maxLevel = level;
  }

  /**
   * Physically-based glare effects for digital images.
   * https://www.graphics.cornell.edu/pubs/1995/SSZG95.html
   */
  public float filter(float x, float y, float r0) {
    double r = Math.sqrt(x * x + y * y);
    return (float) Math.pow(0.02 / (r / r0 + 0.02), 3.0);
  }

  public int idx(int x, int y) {
    x = Math.abs(x);
    y = Math.abs(y);
    if (x < y) {
      int tmp = x;
      x = y;
      y = tmp;
    }
    return (x * (x + 1)) / 2 + y;
  }

  public FloatImage downsample(FloatImage image, int size, int border, Mesh mesh) {
    FloatImage res = new FloatImage(size + border * 2, size + border * 2, false);
    bloomDownsampleGen.border = border;
    bloomDownsampleGen.size = size;
    bloomDownsampleGen.input = image.getTexture();
    res.porcess(bloomDownsampleGen, mesh);
    return res;
  }

  int mod(int x, int m) {
    int result = x % m;
    return result < 0 ? result + m : result;
  }

  public SizeBorder<KernelVector> upsample(SizeBorder<KernelVector> image) {
    int double_size = image.size * 2;
    int double_border = image.border - 1;
    SizeBorder<KernelVector> res = new SizeBorder<>(double_size, double_border);
    for (int y = -double_border; y < double_size + double_border; ++y) {
      for (int x = -double_border; x < double_size + double_border; ++x) {
        int sx = x <= 0 ? -(2 - x) / 2 : (x - 1) / 2;
        int sy = y <= 0 ? -(2 - y) / 2 : (y - 1) / 2;
        KernelVector c0 = image.get(sx, sy);
        KernelVector c1 = image.get(sx + 1, sy);
        KernelVector c2 = image.get(sx, sy + 1);
        KernelVector c3 = image.get(sx + 1, sy + 1);
        int w = mod(x, 2) + 2 * mod(y, 2);
        assert (w >= 0 && w < 4);
        KernelVector value = c0.cpy().scl(UPSAMPLE[w][0]).add(c1.cpy().scl(UPSAMPLE[w][1]))
            .add(c2.cpy().scl(UPSAMPLE[w][2])).add(c3.cpy().scl(UPSAMPLE[w][3]));
        res.set(x, y, value);
      }
    }
    return res;
  }

  public SizeBorder<KernelVector> convolution(FloatImage img, SizeBorder<KernelVector> kernel,
      int size, int border, int totalUnknowns) {
    SizeBorder<KernelVector> res = new SizeBorder<>(size + border * 2, size + border * 2);
    int k_size = kernel.size / 2;
    for (int y = -border; y < size + border; ++y) {
      for (int x = -border; x < size + border; ++x) {
        KernelVector value = new KernelVector(totalUnknowns, 0f);
        for (int dy = -k_size; dy <= k_size; ++dy) {
          for (int dx = -k_size; dx < k_size; ++dx) {
            if (x + dx >= -border && x + dx < size + border && y + dy >= -border
                && y + dy < size + border) {
              value.add(kernel.get(dx + k_size, dy + k_size).cpy()
                  .scl(img.get(x + dx + border, y + dy + border)));
            }
          }
        }
        res.set(x, y, value);
      }
    }
    return res;
  }

  public void generate(int height, int size) {
    Mesh screen = OCMeshBuilder.screenMesh();
    int levels = getLevels(height);
    int border = 2;
    for (int i = 1; i < levels; ++i) {
      border = 2 * (border + 1);
    }
    float integral = 0.0f;
    float r0 = height / scl;
    for (int y = -border; y < size + border; ++y) {
      for (int x = -border; x < size + border; ++x) {
        integral += filter(x - size / 2, y - size / 2, r0);
      }
    }
    FloatImage target = new FloatImage(size + border * 2, size + border * 2, false);
    bloomFilter.integral = integral;
    bloomFilter.border = border;
    bloomFilter.size = size;
    bloomFilter.r0 = r0;
    target.porcess(bloomFilter, screen);
    int kernelSize = 2;
    int kernelUnknowns = ((kernelSize + 1) * (kernelSize + 2)) / 2;
    int totalUnknowns = levels * kernelUnknowns;
    SizeBorder<KernelVector>[] filterKernels = new SizeBorder[levels];
    for (int i = 0; i < levels; ++i) {
      filterKernels[i] = new SizeBorder<>(2 * kernelSize + 1, 0);
      int offset = i * kernelUnknowns;
      for (int y = -kernelSize; y <= kernelSize; ++y) {
        for (int x = -kernelSize; x <= kernelSize; ++x) {
          KernelVector value = new KernelVector(totalUnknowns, 0.0f);
          for (int index = idx(x, y); index < kernelUnknowns; ++index) {
            value.x[index + offset] = 1.0f;
          }
          filterKernels[i].set(x + kernelSize, y + kernelSize, value);
        }
      }
    }
    FloatImage img_mipmap[] = new FloatImage[levels];
    img_mipmap[0] = new FloatImage(size + border * 2, size + border * 2, false);
    centerPixel.size = size;
    centerPixel.border = border;
    img_mipmap[0].porcess(centerPixel, screen);
    int tsize = size;
    int tborder = border;
    for (int i = 1; i < levels; ++i) {
      tsize = tsize / 2;
      tborder = tborder / 2 - 1;
      img_mipmap[i] = downsample(img_mipmap[i - 1], tsize, tborder, screen);
    }
    SizeBorder<KernelVector>[] conv_img_mipmap = new SizeBorder[levels];
    tsize = size;
    tborder = border;
    for (int i = 0; i < levels; ++i) {
      conv_img_mipmap[i] =
          convolution(img_mipmap[i], filterKernels[i], tsize, tborder, totalUnknowns);
      tsize = tsize / 2;
      tborder = tborder / 2 - 1;
    }
    for (int i = levels - 2; i >= 0; --i) {
      conv_img_mipmap[i].add(upsample(conv_img_mipmap[i + 1]));
    }
    int expand_size = kernelSize;
    for (int i = 1; i < levels; ++i) {
      expand_size = 2 * (expand_size + 1);
    }
    int first_rows = (2 * kernelSize + 1) * (2 * kernelSize + 1);
    int rows = first_rows + expand_size - (kernelSize + 1) + 1;
    int cols = totalUnknowns;
    // TODO It is too difficult to implement this generator.
    // Wait please
    //
    screen.dispose();
    target.dispose();
    for (int i = 0; i < levels; ++i) {
      img_mipmap[i].dispose();
    }
  }
}
