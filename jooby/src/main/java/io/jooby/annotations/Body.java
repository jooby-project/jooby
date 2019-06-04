/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a Mvc parameter to the HTTP body.
 *
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;POST
 *     public void method(&#64;Body MyBean) {
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @since 0.6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER })
public @interface Body {
}
