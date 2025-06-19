/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.QueryString;
import io.jooby.value.ConversionHint;
import io.jooby.value.ValueFactory;

public class QueryStringValue extends HashValue implements QueryString {
  private String queryString;

  public QueryStringValue(ValueFactory valueFactory, String queryString) {
    super(valueFactory);
    this.queryString = queryString;
  }

  @Nullable @Override
  public <T> T toNullable(@NonNull Class<T> type) {
    // NOTE: 2.x backward compatible. Make sure Query object are almost always created
    // GET /search?
    // with class Search (q="*")
    // so q is defaulted to "*"
    return factory.convert(type, this, ConversionHint.Empty);
  }

  @NonNull @Override
  public String queryString() {
    return queryString;
  }
}
