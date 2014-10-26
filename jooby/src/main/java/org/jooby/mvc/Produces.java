package org.jooby.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.BodyConverter;

/**
 * Defines what media types a route can produces. By default a route can produces any type
 * {@code *}/{@code *}.
 * The <code>Accept</code> header is used for finding the best {@link BodyConverter}.
 * If there isn't a {@link BodyConverter} a "406 Not Acceptable" response will be generated.
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
