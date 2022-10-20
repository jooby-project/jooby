/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.exception.MissingValueException;
import jakarta.inject.Provider;
import source.Controller1786b;

public class Expected1786b implements MvcFactory {

  @Override
  public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override
  public Extension create(@NonNull Provider provider) {
    return application -> {
      application.get(
          "/required-param",
          ctx -> {
            Controller1786b controller = (Controller1786b) provider.get();
            UUID uuid = ctx.query("uuid").to(UUID.class);
            return controller.requiredParam(MissingValueException.requireNonNull("uuid", uuid));
          });
    };
  }
}
