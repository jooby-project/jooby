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
