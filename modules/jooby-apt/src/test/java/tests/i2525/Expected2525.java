/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import jakarta.inject.Provider;

public class Expected2525 implements MvcFactory {

  private static void install(Jooby application, Provider<C2525> provider) throws Exception {
    application.get(
        "/2525",
        ctx -> {
          C2525 myController = provider.get();
          return null;
        });
  }

  @Override
  public boolean supports(Class type) {
    return type == C2525.class;
  }

  @Override
  public Extension create(Provider provider) {
    return app -> install(app, provider);
  }
}
