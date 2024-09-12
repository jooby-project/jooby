/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;

public class App3508 extends Jooby {
  public App3508(Extension validator) {
    install(new JacksonModule());
    install(validator);

    mvc(new Controller3508_());
  }
}
