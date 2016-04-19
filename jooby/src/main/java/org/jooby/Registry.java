package org.jooby;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * <h1>service registry</h1>
 * <p>
 * Provides access to services registered by modules or application. The registry is powered by
 * Guice.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public interface Registry {

  /**
   * Request a service of the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final Class<T> type) {
    return require(Key.get(type));
  }

  /**
   * Request a service of the given type and name.
   *
   * @param name A service name.
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final String name, final Class<T> type) {
    return require(Key.get(type, Names.named(name)));
  }

  /**
   * Request a service of the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final TypeLiteral<T> type) {
    return require(Key.get(type));
  }

  /**
   * Request a service of the given key.
   *
   * @param key A service key.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  <T> T require(Key<T> key);

}
