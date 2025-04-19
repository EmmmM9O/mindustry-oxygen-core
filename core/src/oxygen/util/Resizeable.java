/* (C) 2025 */
package oxygen.util;

public interface Resizeable {

  public void resize(int width, int height);

  public void onResize();

  public int getWidth();

  public int getHeight();
}
