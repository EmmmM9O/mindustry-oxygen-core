/* (C) 2025 */
package oxygen.math.geom;

public class PlaneArr<T> {
  T[] val;
  public int width, height;

  public PlaneArr(int width, int height) {
    val = (T[]) (new Object[width * height]);
  }

  public void set(int x, int y, T value) {
    val[x + y * width] = value;
  }

  public T get(int x, int y) {
    return val[x + y * width];
  }
}
