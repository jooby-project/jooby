package io.jooby.internal;

import io.jooby.Context;

import java.util.Optional;
import java.util.function.Function;

public class HiddenMethodInitializer implements ContextInitializer {
  private final Function<Context, Optional<String>> provider;

  public HiddenMethodInitializer(Function<Context, Optional<String>> provider) {
    this.provider = provider;
  }

  @Override public void apply(Context ctx) {
    provider.apply(ctx).ifPresent(ctx::setMethod);
  }
}
