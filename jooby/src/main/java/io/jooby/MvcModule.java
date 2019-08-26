/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.inject.Provider;

public interface MvcModule {
  boolean supports(Class type);

  Extension create(Provider provider);
}
