/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.avaje.jsonb;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.json.JsonDataException;
import io.avaje.jsonb.Jsonb;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.internal.trpc.avaje.jsonb.AvajeTrpcParser;
import io.jooby.trpc.TrpcErrorCode;
import io.jooby.trpc.TrpcParser;

public class TrpcAvajeJsonbModule implements Extension {
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var services = application.getServices();
    // tRpc
    services.put(TrpcParser.class, new AvajeTrpcParser(application.require(Jsonb.class)));
    application.errorCode(JsonDataException.class, StatusCode.BAD_REQUEST);
    services
        .mapOf(Class.class, TrpcErrorCode.class)
        .put(JsonDataException.class, TrpcErrorCode.BAD_REQUEST);
  }
}
