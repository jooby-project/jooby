/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.inject.Provider;

/**
 * Created by a Jooby annotation processor tool and added discovered using the
 * {@link java.util.ServiceLoader} API.
 *
 * @since 2.1.0
 */
public interface MvcFactory {
  /**
   * Check if the factory applies for the given MVC route.
   *
   * @param type MVC route.
   * @return True for matching factory.
   */
  boolean supports(@Nonnull Class type);

  /**
   * Creates an extension module. The extension module are created at compilation time by Jooby
   * APT.
   *
   * @param provider MVC route instance provider.
   * @return All mvc route as extension module.
   */
  @Nonnull Extension create(@Nonnull Provider provider);
}
