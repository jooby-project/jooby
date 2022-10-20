/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1807;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.exception.MissingValueException;
import jakarta.inject.Provider;

public class Expected1807 implements MvcFactory {

  @Override
  public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override
  public Extension create(@NonNull Provider provider) {
    return application -> {
      application.get(
          "/test/{word}",
          ctx -> {
            C1807 controller = (C1807) provider.get();
            Word1807 word = ctx.multipart().to(Word1807.class);
            controller.hello(MissingValueException.requireNonNull("data", word));
            return ctx;
          });
    };
  }
}
