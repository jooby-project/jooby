/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Registry;

public class RegistryRef {

  private Registry registry;

  public boolean isSet() {
    return registry != null;
  }

  public Registry get() {
    return registry;
  }

  public void set(Registry registry) {
    this.registry = registry;
  }
}
