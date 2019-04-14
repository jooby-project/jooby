package io.jooby.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines what media types a route can produces.
 *
 * Check the <code>Accept</code> header against this value or send a "406 Not Acceptable" response.
 *
 * <pre>
 *   class Resources {
 *
 *     &#64;Produces("application/json")
 *     public Object method() {
 *      return ...;
 *     }
 *   }
 * </pre>
 * @author edgar
 * @since 0.1.0
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Produces {
  String[] value();
}
