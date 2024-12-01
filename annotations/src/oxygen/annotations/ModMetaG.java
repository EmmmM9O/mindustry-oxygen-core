/* (C) 2024 */
package oxygen.annotations;

import java.lang.annotation.*;

/**
 * ModMeta
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ModMetaG {
    String name();

    String minGameVersion();

    // Class<?> main() default Mod.class;

    boolean pregenerated() default false;

    boolean hidden() default false;

    boolean keepOutlines() default false;

    boolean java() default true;

    float texturescale() default 1.0f;

    String displayName() default "";

    String author() default "";

    String version() default "";

    String description() default "";

    String repo() default "";

    String subtitle() default "";

    String[] dependencies() default {};
}
