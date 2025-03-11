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
  String withS2() default "";

  /*
   * For EventTypeG true to unmark For TexSG true to not generate style
   */
  boolean withB() default false;
}
