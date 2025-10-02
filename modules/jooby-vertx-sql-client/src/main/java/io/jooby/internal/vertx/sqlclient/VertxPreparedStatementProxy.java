/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.vertx.core.Future;
import io.vertx.core.impl.VertxThread;
import io.vertx.sqlclient.*;

public record VertxPreparedStatementProxy(String name) implements PreparedStatement {
  private PreparedStatement get() {
    var thread = Thread.currentThread();
    if (!(thread instanceof VertxThread)) {
      throw new IllegalStateException("Current thread is not a vertx thread");
    }
    return VertxThreadLocalPreparedObject.<PreparedStatement>get(name).get(0);
  }

  @Override
  public PreparedQuery<RowSet<Row>> query() {
    return new VertxPreparedQueryProxy(name + ".query");
  }

  @Override
  public Cursor cursor() {
    return get().cursor();
  }

  @Override
  public Cursor cursor(Tuple args) {
    return get().cursor(args);
  }

  @Override
  public RowStream<Row> createStream(int fetch) {
    return get().createStream(fetch);
  }

  @Override
  public RowStream<Row> createStream(int fetch, Tuple args) {
    return get().createStream(fetch, args);
  }

  @Override
  public Future<Void> close() {
    return get().close();
  }

  @Override
  @NonNull public String toString() {
    return Thread.currentThread().getName() + ":" + name;
  }
}
