/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.vertx.core.Future;
import io.vertx.core.impl.VertxThread;
import io.vertx.sqlclient.*;

public record VertxPreparedQueryProxy(String name) implements PreparedQuery<RowSet<Row>> {
  private PreparedQuery<RowSet<Row>> get() {
    var thread = Thread.currentThread();
    if (!(thread instanceof VertxThread)) {
      throw new IllegalStateException("Current thread is not a vertx thread");
    }
    return VertxThreadLocalPreparedObject.<PreparedQuery<RowSet<Row>>>get(name).get(0);
  }

  @NonNull public String toString() {
    return Thread.currentThread().getName() + ":" + name;
  }

  @Override
  public Future<RowSet<Row>> execute() {
    return get().execute();
  }

  @Override
  public Future<RowSet<Row>> execute(Tuple tuple) {
    return get().execute(tuple);
  }

  @Override
  public Future<RowSet<Row>> executeBatch(List<Tuple> batch) {
    return get().executeBatch(batch);
  }

  @Override
  public <R> PreparedQuery<SqlResult<R>> collecting(Collector<Row, ?, R> collector) {
    return get().collecting(collector);
  }

  @Override
  public <U> PreparedQuery<RowSet<U>> mapping(Function<Row, U> mapper) {
    return get().mapping(mapper);
  }
}
