/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

import java.util.function.Supplier;

import io.jooby.SneakyThrows;
import jakarta.inject.Provider;

public class SingletonProvider<T> implements Provider<T> {

  private final Object LOCK = new Object();

  protected T instance;

  private Supplier<T> factory;

  private SneakyThrows.Consumer<T> close;

  public SingletonProvider(Supplier<T> factory, SneakyThrows.Consumer<T> close) {
    this.factory = factory;
    this.close = close;
  }

  @Override
  public T get() {
    if (instance != null) {
      return instance;
    }
    synchronized (LOCK) {
      if (instance != null) {
        return instance;
      }
      instance = factory.get();
      return instance;
    }
  }

  public void close() {
    if (instance != null && close != null) {
      close.accept(instance);
    }
  }
}
