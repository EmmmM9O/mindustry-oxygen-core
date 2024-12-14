/* (C) 2024 */
package oxygen.annotations.component;

import java.lang.annotation.*;
import oxygen.annotations.mark.*;

/**
 * RecordGen
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@NoCopy
public @interface AutoComponent {
    ComponentType[] value();

    String path() default "";

    String className() default "";

    String configure() default "{}";
}
