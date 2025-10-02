/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.AbstractList;

import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class VertxPreparedQueryProxyList extends AbstractList<PreparedQuery<RowSet<Row>>> {
  private final String name;

  public VertxPreparedQueryProxyList(String name) {
    this.name = name;
  }

  @Override
  public PreparedQuery<RowSet<Row>> get(int index) {
    return VertxThreadLocalPreparedObject.<PreparedQuery<RowSet<Row>>>get(name).get(index);
  }

  @Override
  public int size() {
    return VertxThreadLocalPreparedObject.get(name).size();
  }

  @Override
  public String toString() {
    return Thread.currentThread().getName() + ":" + name;
  }
}
