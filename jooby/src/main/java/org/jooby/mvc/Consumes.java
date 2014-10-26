package org.jooby.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.BodyConverter;

/**
 * Defines what media types a route can consume. By default a route can consume any type
 * {@code *}/{@code *}.
 * The <code>Content-Type</code> header is used for finding the best {@link BodyConverter}.
 * If there isn't a {@link BodyConverter} a "415 Unsupported Media Type"
 * response will be generated.
 * <pre>
 *   class Resources {
 *
 *     &#64;Consume("application/json")
 *     public void method(&#64;Body MyBody body) {
 *     }
 *   }
 * </pre>
 * @author edgar
 * @since 0.1.0
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Consumes {
  /**
   * @return Media types the route can consume.
   */
  String[] value();
}
