/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.trpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.jooby.trpc.TrpcResponse;

class AvajeTrpcResponseAdapterTest {

  private Jsonb jsonb;
  private JsonWriter writer;
  private JsonReader reader;
  private AvajeTrpcResponseAdapter adapter;

  @BeforeEach
  void setUp() {
    jsonb = mock(Jsonb.class);
    writer = mock(JsonWriter.class);
    reader = mock(JsonReader.class);
    adapter = new AvajeTrpcResponseAdapter(jsonb);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldWriteJsonWithNonNullData() {
    // Arrange
    TrpcResponse<Object> response = mock(TrpcResponse.class);
    Object payloadData = new Object();
    when(response.data()).thenReturn(payloadData);

    JsonAdapter<Object> objectAdapter = mock(JsonAdapter.class);
    when(jsonb.adapter(Object.class)).thenReturn(objectAdapter);

    // Act
    adapter.toJson(writer, response);

    // Assert exact serialization sequence
    InOrder inOrder = inOrder(writer, objectAdapter);
    inOrder.verify(writer).beginObject();
    inOrder.verify(writer).name("result");

    inOrder.verify(writer).beginObject();
    inOrder.verify(writer).name("data");
    inOrder.verify(objectAdapter).toJson(writer, payloadData);

    // We close two objects, so we must verify endObject twice
    inOrder.verify(writer, times(2)).endObject();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldWriteJsonWithNullData() {
    // Arrange
    TrpcResponse<Object> response = mock(TrpcResponse.class);
    when(response.data()).thenReturn(null);

    // Act
    adapter.toJson(writer, response);

    // Assert
    InOrder inOrder = inOrder(writer);
    inOrder.verify(writer).beginObject();
    inOrder.verify(writer).name("result");

    inOrder.verify(writer).beginObject();
    inOrder.verify(writer, never()).name("data");

    // Even if data is null, the result and root objects are still closed
    inOrder.verify(writer, times(2)).endObject();

    verify(jsonb, never()).adapter(Object.class);
  }

  @Test
  void shouldThrowUnsupportedOperationExceptionOnFromJson() {
    // Act & Assert
    UnsupportedOperationException ex =
        assertThrows(UnsupportedOperationException.class, () -> adapter.fromJson(reader));

    assertEquals("Deserialization of TrpcEnvelope is not required", ex.getMessage());
  }
}
