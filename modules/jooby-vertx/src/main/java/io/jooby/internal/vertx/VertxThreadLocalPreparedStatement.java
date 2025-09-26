/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import io.vertx.core.Future;
import io.vertx.sqlclient.*;

public class VertxThreadLocalPreparedStatement implements PreparedStatement {

  private static final ThreadLocal<PreparedStatement> preparedStatement = new ThreadLocal<>();

  static final VertxThreadLocalPreparedStatement INSTANCE = new VertxThreadLocalPreparedStatement();

  static void set(PreparedStatement statement) {
    preparedStatement.set(statement);
  }

  private PreparedStatement current() {
    System.out.println(Thread.currentThread().getName());
    return preparedStatement.get();
  }

  /**
   * Create a prepared query for this statement.
   *
   * @return the prepared query
   */
  public PreparedQuery<RowSet<Row>> query() {
    return current().query();
  }

  /** Like {@link #cursor(Tuple)} but with empty arguments. */
  public Cursor cursor() {
    return current().cursor();
  }

  /**
   * Create a cursor with the provided {@code arguments}.
   *
   * @param args the list of arguments
   * @return the query
   */
  public Cursor cursor(Tuple args) {
    return current().cursor(args);
  }

  /** Like {@link #createStream(int, Tuple)} but with empty arguments. */
  public RowStream<Row> createStream(int fetch) {
    return current().createStream(fetch);
  }

  /**
   * Execute the prepared query with a cursor and createStream the result. The createStream opens a
   * cursor with a {@code fetch} size to fetch the results.
   *
   * <p>Note: this requires to be in a transaction, since cursors require it.
   *
   * @param fetch the cursor fetch size
   * @param args the prepared query arguments
   * @return the createStream
   */
  public RowStream<Row> createStream(int fetch, Tuple args) {
    return current().createStream(fetch, args);
  }

  /** Close the prepared query and release its resources. */
  public Future<Void> close() {
    return current().close();
  }
}
