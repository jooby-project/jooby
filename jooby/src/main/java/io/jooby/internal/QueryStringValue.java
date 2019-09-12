/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.QueryString;

import javax.annotation.Nonnull;

public class QueryStringValue extends HashValue implements QueryString {
  private String queryString;

  public QueryStringValue(Context ctx, String queryString) {
    super(ctx);
    this.queryString = queryString;
  }

  @Nonnull @Override public String queryString() {
    return queryString;
  }
}
