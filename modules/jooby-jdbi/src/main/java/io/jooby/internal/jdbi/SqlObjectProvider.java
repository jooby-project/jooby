/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jdbi;

import org.jdbi.v3.core.Handle;

import javax.inject.Provider;

public class SqlObjectProvider implements Provider {
  private Provider<Handle> handle;

  private Class type;

  public SqlObjectProvider(Provider<Handle> handle, Class type) {
    this.handle = handle;
    this.type = type;
  }

  @Override public Object get() {
    Handle handle = this.handle.get();
    return handle.attach(type);
  }
}
