/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * Bean value converter, works like {@link ValueConverter} except that the input value is always
 * a hash/object value, not a simple string like it is required by {@link ValueConverter}.
 *
 * @since 2.1.1
 */
public interface BeanConverter extends ValueConverter {
}
