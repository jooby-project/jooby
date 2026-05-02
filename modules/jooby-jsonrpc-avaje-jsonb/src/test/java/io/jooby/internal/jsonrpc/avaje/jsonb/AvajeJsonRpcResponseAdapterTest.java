/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc.avaje.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.jooby.jsonrpc.JsonRpcResponse;

@ExtendWith(MockitoExtension.class)
class AvajeJsonRpcResponseAdapterTest {

  @Mock Jsonb jsonb;
  @Mock JsonWriter writer;
  @Mock JsonReader reader;
  @Mock JsonAdapter<Object> objectAdapter;
  @Mock JsonRpcResponse response;

  private AvajeJsonRpcResponseAdapter adapter;

  @BeforeEach
  void setup() {
    adapter = new AvajeJsonRpcResponseAdapter(jsonb);
  }

  // --- SERIALIZATION (TO JSON) TESTS ---

  @Test
  void testToJson_WithError() {
    JsonRpcResponse.ErrorDetail errorObj = mock(JsonRpcResponse.ErrorDetail.class);

    // Setup response to have an error
    when(response.getError()).thenReturn(errorObj);
    when(response.getId()).thenReturn("req-id");

    // Mock the inner writePOJO call's dependency
    when(jsonb.adapter(Object.class)).thenReturn(objectAdapter);

    adapter.toJson(writer, response);

    verify(writer).beginObject();
    verify(writer).name("jsonrpc");
    verify(writer).value("2.0");

    // Verifies the error branch
    verify(writer).name("error");
    verify(objectAdapter).toJson(writer, errorObj);

    verify(writer).name("id");
    verify(writer).jsonValue("req-id");
    verify(writer).endObject();
  }

  @Test
  void testToJson_WithNonNullResult() {
    Object resultObj = new Object();

    // Setup response to have a successful result (no error)
    when(response.getError()).thenReturn(null);
    when(response.getResult()).thenReturn(resultObj);
    when(response.getId()).thenReturn(42);

    // Mock the inner writePOJO call's dependency
    when(jsonb.adapter(Object.class)).thenReturn(objectAdapter);

    adapter.toJson(writer, response);

    verify(writer).beginObject();
    verify(writer).name("jsonrpc");
    verify(writer).value("2.0");

    // Verifies the successful non-null result branch
    verify(writer).name("result");
    verify(objectAdapter).toJson(writer, resultObj);

    verify(writer).name("id");
    verify(writer).jsonValue(42);
    verify(writer).endObject();
  }

  @Test
  void testToJson_WithNullResult() {
    // Setup response to have neither an error nor a result
    when(response.getError()).thenReturn(null);
    when(response.getResult()).thenReturn(null);
    when(response.getId()).thenReturn(null);

    adapter.toJson(writer, response);

    verify(writer).beginObject();
    verify(writer).name("jsonrpc");
    verify(writer).value("2.0");

    // Verifies the null result branch
    verify(writer).name("result");
    verify(writer).nullValue();

    verify(writer).name("id");
    verify(writer).jsonValue(null);
    verify(writer).endObject();
  }

  // --- DESERIALIZATION (FROM JSON) TESTS ---

  @Test
  void testFromJson_ThrowsUnsupportedOperationException() {
    UnsupportedOperationException ex =
        assertThrows(UnsupportedOperationException.class, () -> adapter.fromJson(reader));

    assertEquals("Servers don't deserialize responses", ex.getMessage());
  }
}
