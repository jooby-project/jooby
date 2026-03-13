/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.rpc.trpc.TrpcResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class JacksonTrpcResponseSerializer extends StdSerializer<TrpcResponse> {
  public JacksonTrpcResponseSerializer() {
    super(TrpcResponse.class);
  }

  @Override
  public void serialize(TrpcResponse value, JsonGenerator gen, SerializationContext provider)
      throws JacksonException {
    gen.writeStartObject(); // {

    gen.writeName("result");
    gen.writeStartObject(); // "result": {

    var data = value.data();
    // Only write the "data" key if the method actually returned something (not void/Unit)
    if (data != null) {
      gen.writeName("data");
      gen.writePOJO(data);
    }

    gen.writeEndObject();
    gen.writeEndObject();
  }
}
