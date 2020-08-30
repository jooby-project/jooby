/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.metrics;

import io.jooby.Context;

public class NoCacheHeader {

  public static void add(Context ctx) {
    ctx.setResponseHeader("Cache-Control", "must-revalidate,no-cache,no-store");
  }
}
