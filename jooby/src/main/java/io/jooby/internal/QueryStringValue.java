/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.QueryString;

public class QueryStringValue extends HashValue implements QueryString {
  private String queryString;

  public QueryStringValue(Context ctx, String queryString) {
    super(ctx);
    this.queryString = queryString;
  }

  protected boolean allowEmptyBean() {
    return true;
  }

  @Override
  protected <T> T toNullable(@NonNull Context ctx, @NonNull Class<T> type, boolean allowEmpty) {
    // NOTE: 2.x backward compatible. Make sure Query object are almost always created
    // GET /search?
    // with class Search (q="*")
    // so q is defaulted to "*"
    return ValueConverters.convert(this, type, ctx.getRouter(), allowEmpty);
  }

  @NonNull @Override
  public String queryString() {
    return queryString;
  }
}
