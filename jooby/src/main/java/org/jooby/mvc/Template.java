package org.jooby.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a view's name to use from a MVC method.
 * <pre>
 *   class Resources {
 *
 *     &#64;Template("view")
 *     public Object method() {
 *       return ...;
 *     }
 *   }
 * </pre>
 * @author edgar
 * @since 0.1.0
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {

  /**
   * @return View name to render.
   */
  String value() default "";

}
