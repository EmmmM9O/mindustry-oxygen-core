package oxygen.core.loader;

import java.lang.annotation.*;

import oxygen.annotations.*;

/**
 * OL
 */
public @interface ML {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @AMark
  public @interface AutoService {
    Class<?> value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @AMark
  public @interface ModInstance {
    String value();
  }
}
