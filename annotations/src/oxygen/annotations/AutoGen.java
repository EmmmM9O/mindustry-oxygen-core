/* (C) 2024 */
package oxygen.annotations;

import java.lang.annotation.*;

/**
 * RecordGen
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoGen {
    GenType value();

    String path() default "";

    String className() default "";
    String with() default "";
    /*
     * For EventTypeG true to unmark
     * */
    boolean withB() default false;
    
}
