/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.jooby.trpc.TrpcResponse;

public class JacksonTrpcResponseSerializer extends StdSerializer<TrpcResponse> {
  public JacksonTrpcResponseSerializer() {
    super(TrpcResponse.class);
  }

  @Override
  public void serialize(TrpcResponse value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();

    gen.writeFieldName("result");
    gen.writeStartObject();

    var data = value.data();
    // Only write the "data" key if the method actually returned something (not void/Unit)
    if (data != null) {
      gen.writeFieldName("data");
      gen.writePOJO(data);
    }

    gen.writeEndObject();
    gen.writeEndObject();
  }
}
