/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2023 Edgar Espina
 */
package io.jooby.jstachio;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Environment;
import io.jstach.jstachio.spi.JStachioConfig;

class JoobyJStachioConfig implements JStachioConfig {
  
  private final Environment environment;
  
  public JoobyJStachioConfig(Environment environment) {
    super();
    this.environment = environment;
  }

  @Override
  public @Nullable String getProperty(@NonNull String key) {
    return environment.getProperty(key);
  }

}
