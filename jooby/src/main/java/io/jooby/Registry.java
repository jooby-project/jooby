/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Service locator pattern which may be provided by a dependency injection framework.
 *
 * @since 2.0.0
 * @author edgar
 * @see ServiceRegistry
 */
public interface Registry {
  /**
   * Provides an instance of the given type.
   *
   * @param type Object type.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @Nonnull <T> T require(@Nonnull Class<T> type) throws RegistryException;

  /**
   * Provides an instance of the given type where name matches it.
   *
   * @param type Object type.
   * @param name Object name.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @Nonnull <T> T require(@Nonnull Class<T> type, @Nonnull String name) throws RegistryException;

  /**
   * Provides an instance of the given type.
   *
   * @param type Object type.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @Nonnull <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException;
}
