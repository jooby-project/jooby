/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.util.Map;

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
      if (data instanceof Iterable<?> iterable) {
        writer.beginArray();
        for (var item : iterable) {
          if (item == null) {
            writer.nullValue();
          } else {
            writePOJO(writer, item);
          }
        }
        writer.endArray();
      } else if (data instanceof Map<?, ?> map) {
        writer.beginObject();
        for (var entry : map.entrySet()) {
          // JSON keys must be strings
          writer.name(String.valueOf(entry.getKey()));

          var value = entry.getValue();
          if (value == null) {
            writer.nullValue();
          } else {
            writePOJO(writer, value);
          }
        }
        writer.endObject();
      } else {
        writePOJO(writer, data);
      }
    }

    writer.endObject();
    writer.endObject();
  }

  private void writePOJO(JsonWriter writer, Object item) {
    // Look up the adapter for the specific element (e.g., Movie.class)
    @SuppressWarnings("unchecked")
    JsonAdapter<Object> itemAdapter = (JsonAdapter<Object>) jsonb.adapter(item.getClass());
    itemAdapter.toJson(writer, item);
  }

  @Override
  public TrpcResponse<?> fromJson(JsonReader reader) {
    // We only serialize out, tRPC clients don't send this envelope to the server
    throw new UnsupportedOperationException("Deserialization of TrpcEnvelope is not required");
  }
}
