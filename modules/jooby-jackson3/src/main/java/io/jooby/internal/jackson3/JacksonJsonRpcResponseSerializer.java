/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import io.jooby.rpc.jsonrpc.JsonRpcResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class JacksonJsonRpcResponseSerializer extends StdSerializer<JsonRpcResponse> {

  public JacksonJsonRpcResponseSerializer() {
    super(JsonRpcResponse.class);
  }

  @Override
  public void serialize(JsonRpcResponse response, JsonGenerator gen, SerializationContext ctxt)
      throws JacksonException {
    gen.writeStartObject();
    gen.writeName("jsonrpc");
    gen.writeString("2.0");

    if (response.getError() != null) {
      gen.writeName("error");
      gen.writePOJO(response.getError());
    } else {
      gen.writeName("result");
      gen.writePOJO(response.getResult());
    }
    if (response.getId() == null) {
      gen.writeNullProperty("id");
    } else {
      gen.writePOJOProperty("id", response.getId());
    }

    gen.writeEndObject();
  }
}
