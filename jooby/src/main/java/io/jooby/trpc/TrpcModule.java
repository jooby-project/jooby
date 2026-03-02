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
  public void install(@NonNull Jooby application) throws Exception {
    application.error(new TrpcErrorHandler());
  }
}
