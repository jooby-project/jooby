/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1859;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.Route;
import jakarta.inject.Provider;

public class Expected1859 implements MvcFactory {

  @Override
  public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override
  public Extension create(@NonNull Provider provider) {
    return application -> {
      Route route =
          application.get(
              "/c/1859",
              ctx -> {
                C1859 controller = (C1859) provider.get();
                String value = ctx.body().valueOrNull();
                return controller.foo(value);
              });
      route.setReturnType(String.class);
    };
  }
}
