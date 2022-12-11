/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.lang.reflect.Type;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Reified;
import io.jooby.ResultHandler;
import io.jooby.Route;

class RockerResultHandler implements ResultHandler {
  private final RockerOutputFactory<ByteBufferOutput> factory;

  RockerResultHandler(final RockerOutputFactory<ByteBufferOutput> factory) {
    this.factory = factory;
  }

  @Override
  public boolean matches(@NonNull Type type) {
    return RockerModel.class.isAssignableFrom(Reified.rawType(type));
  }

  @Override
  public @NonNull Route.Filter create() {
    return new RockerHandler(factory);
  }

  @Override
  public boolean isReactive() {
    return false;
  }
}
