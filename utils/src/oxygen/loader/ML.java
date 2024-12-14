/* (C) 2024 */
package oxygen.loader;

import java.lang.annotation.*;
import oxygen.annotations.mark.*;
import oxygen.utils.*;

/**
 * OL
 */
public @interface ML {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @AMark
    public @interface Event {
        int value() default StandardEventPriority.NORMAL;

        String event() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @AMark
    public @interface Instance {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @AMark
    public @interface EventType {}
}
