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
import io.jooby.jsonrpc.JsonRpcResponse;

public class AvajeJsonRpcResponseAdapter implements JsonAdapter<JsonRpcResponse> {

  private final Jsonb jsonb;

  public AvajeJsonRpcResponseAdapter(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public void toJson(JsonWriter writer, JsonRpcResponse response) {
    writer.beginObject();

    writer.name("jsonrpc");
    writer.value("2.0");

    if (response.getError() != null) {
      writer.name("error");
      writePOJO(writer, response.getError());
    } else {
      writer.name("result");
      if (response.getResult() == null) {
        writer.nullValue();
      } else {
        writePOJO(writer, response.getResult());
      }
    }

    writer.name("id");
    var id = response.getId();
    writer.jsonValue(id);

    writer.endObject();
  }

  private void writePOJO(JsonWriter writer, Object item) {
    JsonAdapter<Object> itemAdapter = jsonb.adapter(Object.class);
    itemAdapter.toJson(writer, item);
  }

  @Override
  public JsonRpcResponse fromJson(JsonReader reader) {
    throw new UnsupportedOperationException("Servers don't deserialize responses");
  }
}
