/* (C) 2024 */
package oxygen.annotations.generator;

import java.lang.annotation.*;
import oxygen.annotations.mark.*;

/**
 * RecordGen
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@NoCopy
public @interface AutoGen {
    GenType value();

    String path() default "";

    String className() default "";

    String withS() default "";

    /*
     * For EventTypeG true to unmark
     */
    boolean withB() default false;
}
