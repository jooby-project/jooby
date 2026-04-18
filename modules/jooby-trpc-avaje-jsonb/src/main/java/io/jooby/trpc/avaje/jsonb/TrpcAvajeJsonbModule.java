/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.avaje.jsonb;

import io.avaje.json.JsonDataException;
import io.avaje.jsonb.Jsonb;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.internal.trpc.avaje.jsonb.AvajeTrpcParser;
import io.jooby.trpc.TrpcErrorCode;
import io.jooby.trpc.TrpcParser;

/**
 * Implementation of jooby-trpc using avaje-jsonb. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new AvajeJsonbModule());
 *   install(new TrpcAvajeJsonbModule());
 *   install(new TrpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
public class TrpcAvajeJsonbModule implements Extension {
  @Override
  public void install(Jooby application) throws Exception {
    var services = application.getServices();
    // tRpc
    services.put(TrpcParser.class, new AvajeTrpcParser(application.require(Jsonb.class)));
    application.errorCode(JsonDataException.class, StatusCode.BAD_REQUEST);
    services
        .mapOf(Class.class, TrpcErrorCode.class)
        .put(JsonDataException.class, TrpcErrorCode.BAD_REQUEST);
  }
}
