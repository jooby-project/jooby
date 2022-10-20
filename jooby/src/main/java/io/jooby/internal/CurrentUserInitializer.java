/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.function.Function;

import io.jooby.Context;

public class CurrentUserInitializer implements ContextInitializer {
  private final Function<Context, Object> provider;

  public CurrentUserInitializer(Function<Context, Object> provider) {
    this.provider = provider;
  }

  @Override
  public void apply(Context ctx) {
    Object user = provider.apply(ctx);
    ctx.setUser(user);
  }
}
