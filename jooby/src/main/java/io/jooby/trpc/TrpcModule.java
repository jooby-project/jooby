/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;

public class TrpcModule implements Extension {
  @Override
  public void install(@NonNull Jooby app) throws Exception {
    var services = app.getServices();

    services.require(TrpcParser.class);
    // Custom mapping for TrpcErrorCode
    services.mapOf(Class.class, TrpcErrorCode.class);

    app.error(new TrpcErrorHandler());
  }
}
