/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson3;

import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;
import tools.jackson.databind.module.SimpleModule;

public class JacksonJsonRpcModule extends SimpleModule {

  public JacksonJsonRpcModule() {
    super("json-rpc");
    addSerializer(JsonRpcResponse.class, new JacksonJsonRpcResponseSerializer());
    addDeserializer(JsonRpcRequest.class, new JacksonJsonRpcRequestDeserializer());
  }
}
