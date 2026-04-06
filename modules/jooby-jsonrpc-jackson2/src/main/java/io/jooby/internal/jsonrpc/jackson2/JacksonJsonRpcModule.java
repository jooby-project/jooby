/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.jackson2;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;

public class JacksonJsonRpcModule extends SimpleModule {

  public JacksonJsonRpcModule() {
    super("json-rpc");
    addDeserializer(JsonRpcRequest.class, new JacksonJsonRpcRequestDeserializer());
    addSerializer(JsonRpcResponse.class, new JacksonJsonRpcResponseSerializer());
  }
}
