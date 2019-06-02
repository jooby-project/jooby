/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import io.jooby.Reified;
import io.jooby.ResponseHandler;
import io.jooby.Route;

import java.lang.reflect.Type;

class RockerResponseHandler implements ResponseHandler {
  @Override public boolean matches(Type type) {
    return RockerModel.class.isAssignableFrom(Reified.rawType(type));
  }

  @Override public Route.Handler create(Route.Handler next) {
    return new RockerHandler(next);
  }
}
