/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import io.jooby.Projected;
import io.jooby.Projection;

class JacksonProjectedSerializerTest {

  private ObjectMapper mapper;
  private JacksonProjectedSerializer serializer;
  private JsonGenerator jsonGenerator;
  private SerializerProvider serializerProvider;

  @BeforeEach
  void setUp() {
    mapper = mock(ObjectMapper.class);
    serializer = new JacksonProjectedSerializer(mapper);
    jsonGenerator = mock(JsonGenerator.class);
    serializerProvider = mock(SerializerProvider.class);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void shouldSerializeAndCacheWriter() throws IOException {
    // Arrange
    Projected projected = mock(Projected.class);
    Projection projection = mock(Projection.class);
    Object value = new Object();

    when(projected.getProjection()).thenReturn(projection);
    when(projected.getValue()).thenReturn(value);

    ObjectWriter writer = mock(ObjectWriter.class);
    when(mapper.writer(any(FilterProvider.class))).thenReturn(writer);

    // Act 1 (Cache Miss)
    serializer.serialize(projected, jsonGenerator, serializerProvider);

    // Assert 1
    verify(mapper, times(1)).writer(any(FilterProvider.class));
    verify(writer, times(1)).writeValue(jsonGenerator, value);

    // Act 2 (Cache Hit)
    serializer.serialize(projected, jsonGenerator, serializerProvider);

    // Assert 2
    // mapper.writer() should NOT be called again due to the writerCache
    verify(mapper, times(1)).writer(any(FilterProvider.class));
    // writeValue SHOULD be called again
    verify(writer, times(2)).writeValue(jsonGenerator, value);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void shouldPropagateIOException() throws IOException {
    // Arrange
    Projected projected = mock(Projected.class);
    Projection projection = mock(Projection.class);
    Object value = new Object();

    when(projected.getProjection()).thenReturn(projection);
    when(projected.getValue()).thenReturn(value);

    ObjectWriter writer = mock(ObjectWriter.class);
    when(mapper.writer(any(FilterProvider.class))).thenReturn(writer);

    IOException expectedException = new IOException("Write failed");
    doThrow(expectedException).when(writer).writeValue(jsonGenerator, value);

    // Act & Assert
    IOException thrown =
        assertThrows(
            IOException.class,
            () -> serializer.serialize(projected, jsonGenerator, serializerProvider));
    assertEquals(expectedException, thrown);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void shouldCreateSeparateWritersForDifferentProjections() throws IOException {
    // Arrange
    Projected projected1 = mock(Projected.class);
    Projection projection1 = mock(Projection.class);
    Object value1 = new Object();
    when(projected1.getProjection()).thenReturn(projection1);
    when(projected1.getValue()).thenReturn(value1);

    Projected projected2 = mock(Projected.class);
    Projection projection2 = mock(Projection.class);
    Object value2 = new Object();
    when(projected2.getProjection()).thenReturn(projection2);
    when(projected2.getValue()).thenReturn(value2);

    ObjectWriter writer1 = mock(ObjectWriter.class);
    ObjectWriter writer2 = mock(ObjectWriter.class);

    // Return writer1 on first call, writer2 on second call
    when(mapper.writer(any(FilterProvider.class))).thenReturn(writer1, writer2);

    // Act
    serializer.serialize(projected1, jsonGenerator, serializerProvider);
    serializer.serialize(projected2, jsonGenerator, serializerProvider);

    // Assert
    // Because the projections are different, the cache is bypassed and writer is called twice
    verify(mapper, times(2)).writer(any(FilterProvider.class));
    verify(writer1, times(1)).writeValue(jsonGenerator, value1);
    verify(writer2, times(1)).writeValue(jsonGenerator, value2);
  }
}
