/* (C) 2024 */
package oxygen.annotations.component;

import java.lang.annotation.*;
import oxygen.annotations.mark.*;

/**
 * With
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@NoCopy
public @interface With {
    OperationType[] value();

    String path() default "";

    String className() default "";

    String configure() default "resource:comp_configure.json:default";
}
