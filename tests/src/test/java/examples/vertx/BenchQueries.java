/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import io.jooby.vertx.VertxPreparedStatement;

public enum BenchQueries implements VertxPreparedStatement {
  SELECT_WORLD("SELECT id, randomnumber from WORLD where id=$1");

  private final String sql;

  BenchQueries(String sql) {
    this.sql = sql;
  }

  @Override
  public String sql() {
    return sql;
  }
}
