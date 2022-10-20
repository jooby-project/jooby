/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.exception.RegistryException;

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
  @NonNull <T> T require(@NonNull Class<T> type) throws RegistryException;

  /**
   * Provides an instance of the given type where name matches it.
   *
   * @param type Object type.
   * @param name Object name.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @NonNull <T> T require(@NonNull Class<T> type, @NonNull String name) throws RegistryException;

  /**
   * Provides an instance of the given type.
   *
   * @param key Object key.
   * @param <T> Object type.
   * @return Instance of this type.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  @NonNull <T> T require(@NonNull ServiceKey<T> key) throws RegistryException;
}
