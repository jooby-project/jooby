package org.jooby.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.BodyConverter;

/**
 * Indicates that a method parameter should be bound to the HTTP request body. The request body
 * will be parsed in a {@link BodyConverter} using the <code>Content-Type</code> header.
 * <pre>
 *   class Resources {
 *
 *     public void method(&#64;Body MyBody body) {
 *     }
 *   }
 * </pre>
 * @author edgar
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {

}
