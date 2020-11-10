/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.lang.reflect.Type;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import io.jooby.Reified;
import io.jooby.ResponseHandler;
import io.jooby.Route;

class RockerResponseHandler implements ResponseHandler {
  private final RockerOutputFactory<ByteBufferOutput> factory;

  RockerResponseHandler(final RockerOutputFactory<ByteBufferOutput> factory) {
    this.factory = factory;
  }

  @Override public boolean matches(Type type) {
    return RockerModel.class.isAssignableFrom(Reified.rawType(type));
  }

  @Override public Route.Handler create(Route.Handler next) {
    return new RockerHandler(next, factory);
  }
}
