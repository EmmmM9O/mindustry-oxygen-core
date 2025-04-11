/* (C) 2025 */
package oxygen.math.geom;

import oxygen.math.*;

/**
 * SizeBorder
 */
public class SizeBorder<T extends Operatorable<T>> {
  public int size, border;
  public T[] val;

  public SizeBorder(int size, int border) {
    this.size = size;
    this.border = border;
    val = (T[]) (new Object[(size + 2 * border) * (size + 2 * border)]);
  }

  public void set(int x, int y, T value) {
    val[x + border + y * (size + border * 2)] = value;
  }

  public T get(int x, int y) {
    return val[x + border + y * (size + border * 2)];
  }

  public SizeBorder<T> add(SizeBorder<T> other) {
    for (int y = -border; y < size + border; ++y) {
      for (int x = -border; x < size + border; ++x) {
        if (x >= -other.border && x < size + other.border && y >= -other.border
            && y < size + other.border) {
          set(x, y, get(x, y).cpy().add(other.get(x, y)));
        } else {
          set(x, y, get(x, y));
        }
      }
    }
    return this;
  }
}
