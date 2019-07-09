/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jdbi;

import io.jooby.RequestScope;
import io.jooby.jdbi.TransactionalRequest;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Provider;

public class SqlObjectProvider implements Provider {
  private Jdbi jdbi;
  private Class type;

  public SqlObjectProvider(Jdbi jdbi, Class type) {
    this.jdbi = jdbi;
    this.type = type;
  }

  @Override public Object get() {
    Handle handle = RequestScope.get(jdbi);
    if (handle == null) {
      // TODO: Replace with a Usage exception
      throw new IllegalStateException(
          "No handle was attached to current request. Make sure `" + TransactionalRequest.class
              .getName() + "` was installed it");
    }
    return handle.attach(type);
  }
}
