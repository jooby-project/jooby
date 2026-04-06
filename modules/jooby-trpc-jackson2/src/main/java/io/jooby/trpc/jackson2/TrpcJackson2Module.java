/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.jackson2;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.trpc.jackson2.JacksonTrpcParser;
import io.jooby.internal.trpc.jackson2.JacksonTrpcResponseSerializer;
import io.jooby.trpc.TrpcParser;
import io.jooby.trpc.TrpcResponse;

public class TrpcJackson2Module implements Extension {
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var services = application.getServices();
    services.put(TrpcParser.class, new JacksonTrpcParser(application.require(ObjectMapper.class)));
    var rpc = new SimpleModule();
    rpc.addSerializer(TrpcResponse.class, new JacksonTrpcResponseSerializer());
    services.listOf(Module.class).add(rpc);
  }
}
