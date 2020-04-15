/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;

import java.util.ArrayList;
import java.util.List;

public class ContextInitializerList implements ContextInitializer {
  private List<ContextInitializer> initializers = new ArrayList<>(5);

  public ContextInitializerList(ContextInitializer initializer) {
    add(initializer);
  }

  @Override public ContextInitializer add(ContextInitializer initializer) {
    if (!initializers.contains(initializer)) {
      initializers.add(initializer);
    }
    return this;
  }

  @Override public void apply(Context ctx) {
    for (ContextInitializer initializer : initializers) {
      initializer.apply(ctx);
    }
  }

  public void remove(ContextInitializer initializer) {
    initializers.remove(initializer);
  }
}
