/* (C) 2025 */
package oxygen.math.geom;

import oxygen.math.*;

public class KernelVector implements Operatorable<KernelVector> {
  public int size;
  public float x[];

  public KernelVector(int size) {
    this.size = size;
    x = new float[size];
  }

  public KernelVector(int size, float value) {
    this(size);
    for (int i = 0; i < size; i++) {
      x[i] = value;
    }
  }

  public KernelVector(KernelVector vec) {
    this(vec.size);
    for (int i = 0; i < size; i++) {
      x[i] = vec.x[i];
    }
  }

  public KernelVector scl(float scalar) {
    for (int i = 0; i < size; i++) {
      x[i] *= scalar;
    }
    return this;
  }

  @Override
  public KernelVector add(KernelVector vec) {
    for (int i = 0; i < size; i++) {
      x[i] += vec.x[i];
    }
    return this;
  }

  @Override
  public KernelVector cpy() {
    return new KernelVector(this);
  }
}
