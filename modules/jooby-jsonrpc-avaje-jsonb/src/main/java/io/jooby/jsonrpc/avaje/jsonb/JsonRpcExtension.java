/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.avaje.jsonb;

import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.JsonbComponent;
import io.jooby.internal.jsonrpc.avaje.jsonb.AvajeJsonRpcErrorAdapter;
import io.jooby.internal.jsonrpc.avaje.jsonb.AvajeJsonRpcRequestAdapter;
import io.jooby.internal.jsonrpc.avaje.jsonb.AvajeJsonRpcResponseAdapter;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;

public class JsonRpcExtension implements JsonbComponent {
  @Override
  public void register(Jsonb.Builder jsonb) {
    jsonb.add(JsonRpcRequest.class, AvajeJsonRpcRequestAdapter::new);
    jsonb.add(JsonRpcResponse.class, AvajeJsonRpcResponseAdapter::new);
    jsonb.add(JsonRpcResponse.ErrorDetail.class, AvajeJsonRpcErrorAdapter::new);
  }
}
