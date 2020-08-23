/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.RequestScope;

import javax.inject.Provider;

import static io.jooby.RequestScope.bind;
import static io.jooby.RequestScope.unbind;

public class ContextAsServiceInitializer implements ContextInitializer, Provider<Context> {

  public static final ContextAsServiceInitializer INSTANCE = new ContextAsServiceInitializer();

  private ContextAsServiceInitializer() {}

  @Override
  public void apply(Context ctx) {
    bind(this, ctx);
    ctx.onComplete(context -> unbind(this));
  }

  @Override
  public Context get() {
    return RequestScope.get(this);
  }
}
