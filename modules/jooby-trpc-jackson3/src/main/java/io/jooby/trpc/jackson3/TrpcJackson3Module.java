/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.jackson3;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.trpc.jackson3.JacksonTrpcParser;
import io.jooby.internal.trpc.jackson3.JacksonTrpcResponseSerializer;
import io.jooby.trpc.TrpcErrorCode;
import io.jooby.trpc.TrpcParser;
import io.jooby.trpc.TrpcResponse;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;

public class TrpcJackson3Module implements Extension {
  @Override
  public void install(@NonNull Jooby application) {
    var services = application.getServices();
    // tRPC error codes
    services
        .mapOf(Class.class, TrpcErrorCode.class)
        .put(StreamReadException.class, TrpcErrorCode.BAD_REQUEST)
        .put(MismatchedInputException.class, TrpcErrorCode.BAD_REQUEST)
        .put(DatabindException.class, TrpcErrorCode.BAD_REQUEST);
    var trpcParser = new JacksonTrpcParser();
    services.put(TrpcParser.class, trpcParser);
    var rpc = new SimpleModule();
    rpc.addSerializer(TrpcResponse.class, new JacksonTrpcResponseSerializer());
    services.listOf(JacksonModule.class).add(rpc);
    // bind to final mapper
    application.onStarting(() -> trpcParser.setMapper(application.require(ObjectMapper.class)));
  }
}
