/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3551;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.jooby.Environment;

public class Service3551 extends SimpleModule {
  private Environment env;

  public Service3551(Environment env) {
    this.env = env;
  }
}
