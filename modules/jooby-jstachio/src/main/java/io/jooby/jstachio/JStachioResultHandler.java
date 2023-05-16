/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.lang.reflect.Type;

import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route.Filter;
import io.jstach.jstachio.JStachio;

class JStachioResultHandler implements ResultHandler {

  private final JStachio jstachio;
  private final JStachioBuffer buffer;

  public JStachioResultHandler(JStachio jstachio, JStachioBuffer buffer) {
    super();
    this.jstachio = jstachio;
    this.buffer = buffer;
  }

  @Override
  public boolean matches(Type type) {
    return jstachio.supportsType(Reified.rawType(type));
  }

  @Override
  public boolean isReactive() {
    return false;
  }

  @Override
  public Filter create() {
    return new JStachioHandler(jstachio, buffer);
  }
}
