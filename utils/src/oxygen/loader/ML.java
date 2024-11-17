/* (C) 2024 */
package oxygen.loader;

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
    public @interface Instance {
        String value();
    }
}
