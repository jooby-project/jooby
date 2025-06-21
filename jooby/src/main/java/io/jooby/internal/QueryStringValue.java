/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.QueryString;
import io.jooby.value.ConversionHint;
import io.jooby.value.ValueFactory;

public class QueryStringValue extends HashValue implements QueryString {
  private final String queryString;

  public QueryStringValue(ValueFactory valueFactory, String queryString) {
    super(valueFactory);
    this.queryString = queryString;
  }

  @Override
  public @NonNull <T> T toEmpty(@NonNull Class<T> type) {
    return factory.convert(type, this, ConversionHint.Empty);
  }

  @NonNull @Override
  public String queryString() {
    return queryString;
  }
}
