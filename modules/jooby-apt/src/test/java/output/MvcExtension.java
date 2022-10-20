/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package output;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MvcFactory;
import jakarta.inject.Provider;
import source.Controller1527;

public class MvcExtension implements MvcFactory {

  private static void install(Jooby application, Provider<MyController> provider) throws Exception {
    application
        .get(
            "/mypath",
            ctx -> {
              MyController myController = provider.get();
              myController.controllerMethod();
              return ctx;
            })
        .attribute("RequireRole", Controller1527.Role.USER);
  }

  @Override
  public boolean supports(@NonNull Class type) {
    return type == MyController.class;
  }

  @NonNull @Override
  public Extension create(@NonNull Provider provider) {
    return app -> install(app, provider);
  }
}
