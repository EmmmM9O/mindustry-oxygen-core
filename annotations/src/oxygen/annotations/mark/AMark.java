/* (C) 2024 */
package oxygen.annotations.mark;

import java.lang.annotation.*;

/**
 * AMark
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface AMark {
  boolean needStatic() default true;
}
