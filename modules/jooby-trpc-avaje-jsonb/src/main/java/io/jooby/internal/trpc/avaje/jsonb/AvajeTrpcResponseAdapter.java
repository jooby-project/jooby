/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.avaje.jsonb;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.jooby.trpc.TrpcResponse;

public class AvajeTrpcResponseAdapter implements JsonAdapter<TrpcResponse> {

  private final Jsonb jsonb;

  public AvajeTrpcResponseAdapter(Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public void toJson(JsonWriter writer, TrpcResponse envelope) {
    writer.beginObject();
    writer.name("result");

    writer.beginObject();
    var data = envelope.data();

    if (data != null) {
      writer.name("data");
      writePOJO(writer, data);
    }

    writer.endObject();
    writer.endObject();
  }

  private void writePOJO(JsonWriter writer, Object item) {
    JsonAdapter<Object> itemAdapter = jsonb.adapter(Object.class);
    itemAdapter.toJson(writer, item);
  }

  @Override
  public TrpcResponse<?> fromJson(JsonReader reader) {
    // We only serialize out, tRPC clients don't send this envelope to the server
    throw new UnsupportedOperationException("Deserialization of TrpcEnvelope is not required");
  }
}
