/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3551;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.jooby.Environment;
import jakarta.inject.Inject;

public class JacksonModule3551 extends SimpleModule {
  private Environment env;

  @Inject
  public JacksonModule3551(Environment env) {
    this.env = env;
  }
}
