/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Formdata;

public class FormdataNode extends HashValue implements Formdata {
  public FormdataNode(Context ctx) {
    super(ctx, null);
  }

  @Override protected String decode(String value) {
    return UrlParser.decode(value, false);
  }
}
