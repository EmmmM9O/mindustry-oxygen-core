/* (C) 2025 */
package oxygen.util;


public interface Copyable<T extends Copyable<T>> {
  public T cpy();
}
