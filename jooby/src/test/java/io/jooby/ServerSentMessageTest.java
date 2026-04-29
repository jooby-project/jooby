/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;

public class ServerSentMessageTest {

  @Test
  @DisplayName("Verify all getters, setters, and null handling")
  public void testProperties() {
    ServerSentMessage msg = new ServerSentMessage("my-data");

    // Set values
    msg.setId(123).setEvent("update").setRetry(5000L);

    // Verify values
    assertEquals("my-data", msg.getData());
    assertEquals("123", msg.getId());
    assertEquals("update", msg.getEvent());
    assertEquals(5000L, msg.getRetry());

    // Verify null handling
    msg.setId(null).setEvent(null).setRetry(null);
    assertNull(msg.getId());
    assertNull(msg.getEvent());
    assertNull(msg.getRetry());
  }

  @Test
  @DisplayName("Verify encoding with id, event, and retry fields populated")
  public void shouldFormatWithAllFields() throws Exception {
    var data = "some-data";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);

    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data).setId(99).setEvent("message").setRetry(1000L);

    String expected = "id:99\nevent:message\nretry:1000\ndata: some-data\n\n";
    assertEquals(
        expected, StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  @Test
  @DisplayName("Verify exception propagation using SneakyThrows")
  public void testExceptionPropagation() throws Exception {
    var ctx = mock(Context.class);
    var route = mock(Route.class);

    when(ctx.getRoute()).thenReturn(route);
    // Simulating an error during encoder retrieval
    when(route.getEncoder()).thenThrow(new RuntimeException("Encoder failed"));

    var message = new ServerSentMessage("data");

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> message.encode(ctx));
    assertEquals("Encoder failed", thrown.getMessage());
  }

  @Test
  @DisplayName("Verify buffer merging logic when data is split across multiple ByteBuffers")
  public void shouldFormatDataAcrossMultipleBuffers() throws Exception {
    var data = "ignored-by-mock";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);

    // We create a mocked Output that simulates data arriving in multiple chunks.
    // Chunk 1: "part1" (No newline, leaves a 'left' tail)
    // Chunk 2: "part2\npart3" (Completes line 1, triggers merge(), starts line 2 leaving a new
    // 'left' tail)
    var chunk1 = ByteBuffer.wrap("part1".getBytes(StandardCharsets.UTF_8));
    var chunk2 = ByteBuffer.wrap("part2\npart3".getBytes(StandardCharsets.UTF_8));

    var outputMock = mock(Output.class);
    when(outputMock.iterator()).thenReturn(List.of(chunk1, chunk2).iterator());

    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(any(Context.class), any())).thenReturn(outputMock);

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);

    // part1 and part2 should be merged into a single "data: " line.
    // part3 should be on its own "data: " line since it was left over after the loop.
    String expected = "data: part1part2\ndata: part3\n\n";
    assertEquals(
        expected, StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  // --- Original Tests Provided by User ---

  @Test
  public void shouldFormatMessage() throws Exception {
    var data = "some";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: " + data + "\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  @Test
  public void shouldFormatMultiLineMessage() throws Exception {
    var data = "line 1\n line ,a .. 2\nline ...abc  3";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: line 1\ndata:  line ,a .. 2\ndata: line ...abc  3\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }

  @Test
  public void shouldFormatMessageEndingWithNL() throws Exception {
    var data = "line 1\n";
    var ctx = mock(Context.class);

    var bufferFactory = OutputFactory.create(OutputOptions.small());
    when(ctx.getOutputFactory()).thenReturn(bufferFactory);
    var encoder = mock(MessageEncoder.class);
    when(encoder.encode(ctx, data))
        .thenReturn(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8)));

    var route = mock(Route.class);
    when(route.getEncoder()).thenReturn(encoder);
    when(ctx.getRoute()).thenReturn(route);

    var message = new ServerSentMessage(data);
    assertEquals(
        "data: " + data + "\n\n",
        StandardCharsets.UTF_8.decode(message.encode(ctx).asByteBuffer()).toString());
  }
}
