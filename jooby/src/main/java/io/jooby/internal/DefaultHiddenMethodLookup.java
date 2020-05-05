/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Router;

import java.util.Optional;
import java.util.function.Function;

public class DefaultHiddenMethodLookup implements Function<Context, Optional<String>> {
  private final String parameterName;

  public DefaultHiddenMethodLookup(String parameterName) {
    this.parameterName = parameterName;
  }

  @Override public Optional<String> apply(Context ctx) {
    if (ctx.getMethod().equals(Router.POST)) {
      return ctx.multipart(parameterName).toOptional();
    }
    return Optional.empty();
  }
}
