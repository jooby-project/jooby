/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.jooby.rpc.jsonrpc.JsonRpcResponse.ErrorDetail;

public class AvajeJsonRpcErrorAdapter implements JsonAdapter<ErrorDetail> {

  private final Jsonb jsonb;

  public AvajeJsonRpcErrorAdapter(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public void toJson(JsonWriter writer, ErrorDetail error) {
    writer.beginObject();

    writer.name("code");
    writer.value(error.getCode());

    writer.name("message");
    writer.value(error.getMessage());

    if (error.getData() != null) {
      writer.name("data");
      jsonb.adapter(Object.class).toJson(writer, error.getData());
    }

    writer.endObject();
  }

  @Override
  public ErrorDetail fromJson(JsonReader reader) {
    throw new UnsupportedOperationException("Servers don't deserialize error responses");
  }
}
