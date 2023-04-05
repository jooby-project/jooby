/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2023 Edgar Espina
 */
package io.jooby.jstachio;

import java.lang.reflect.Type;

import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route.Filter;

class JStachioResultHandler implements ResultHandler {
  
  private final JStachioMessageEncoder encoder;
  
  public JStachioResultHandler(JStachioMessageEncoder encoder) {
    super();
    this.encoder = encoder;
  }

  @Override
  public boolean matches(Type type) {
     return encoder.supportsType(Reified.rawType(type));
  }

  @Override
  public boolean isReactive() {
    return false;
  }

  @Override
  public Filter create() {
    return new JStachioHandler(encoder);
  }

}
