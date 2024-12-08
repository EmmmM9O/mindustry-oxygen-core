/* (C) 2024 */
package oxygen.annotations;

import java.lang.annotation.*;

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
