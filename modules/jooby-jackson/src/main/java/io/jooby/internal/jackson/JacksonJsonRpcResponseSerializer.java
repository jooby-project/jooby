/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.jooby.jsonrpc.JsonRpcResponse;

public class JacksonJsonRpcResponseSerializer extends StdSerializer<JsonRpcResponse> {

  public JacksonJsonRpcResponseSerializer() {
    super(JsonRpcResponse.class);
  }

  @Override
  public void serialize(JsonRpcResponse response, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();

    gen.writeStringField("jsonrpc", "2.0");

    if (response.getError() != null) {
      gen.writeObjectField("error", response.getError());
    } else {
      gen.writeObjectField("result", response.getResult());
    }

    if (response.getId() == null) {
      gen.writeNullField("id");
    } else {
      gen.writeObjectField("id", response.getId());
    }

    gen.writeEndObject();
  }
}
