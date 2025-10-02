/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.AbstractList;

import io.vertx.sqlclient.PreparedStatement;

public class VertxPreparedStatementProxyList extends AbstractList<PreparedStatement> {
  private final String name;

  public VertxPreparedStatementProxyList(String name) {
    this.name = name;
  }

  @Override
  public PreparedStatement get(int index) {
    return VertxThreadLocalPreparedObject.<PreparedStatement>get(name).get(index);
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
