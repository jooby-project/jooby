package org.jooby.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a MVC method parameter as a request header.
 * <pre>
 *   class Resources {
 *
 *     &#64;GET
 *     public void method(&#64;Header String myHeader) {
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @sinde 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Header {
  /**
   * @return Header's name.
   */
  String value() default "";
}
