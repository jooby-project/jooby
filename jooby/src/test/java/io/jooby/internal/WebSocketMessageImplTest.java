/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.Route;
import io.jooby.value.ValueFactory;

public class WebSocketMessageImplTest {

  private Context ctx;
  private Route route;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    route = mock(Route.class);

    // ForwardingContext relies on the wrapped context having a route
    when(ctx.getRoute()).thenReturn(route);
  }

  @Test
  @DisplayName("Test basic byte arrays and ByteBuffer conversions")
  void testBytesAndByteBuffer() {
    byte[] data = "ws-message".getBytes(StandardCharsets.UTF_8);
    WebSocketMessageImpl msg = new WebSocketMessageImpl(ctx, data);

    assertArrayEquals(data, msg.bytes());
    assertEquals(ByteBuffer.wrap(data), msg.byteBuffer());
  }

  @Test
  @DisplayName("Test Value getters (MissingValue and Default fallback)")
  void testValueGetters() {
    ValueFactory factory = new ValueFactory();
    when(ctx.getValueFactory()).thenReturn(factory);

    WebSocketMessageImpl msg = new WebSocketMessageImpl(ctx, new byte[0]);

    // get() returns a MissingValue node
    assertTrue(msg.get("missingKey").isMissing());
    assertEquals("missingKey", msg.get("missingKey").name());

    // getOrDefault()
    assertEquals("fallback", msg.getOrDefault("presentKey", "fallback").value());
  }

  @Test
  @DisplayName("Test decoding payload through to() and toNullable()")
  void testToAndToNullable() throws Exception {
    // 1. Setup the route's consumed media types
    when(route.getConsumes()).thenReturn(Collections.singletonList(MediaType.json));

    // 2. Setup the decoder lookup.
    // DefaultContext.decode(...) uses the Context to find the MessageDecoder
    MessageDecoder decoder = mock(MessageDecoder.class);
    when(decoder.decode(any(Context.class), any(Type.class))).thenReturn("decoded-result");

    // When ForwardingContext asks the underlying context for the decoder, return our mock
    when(ctx.decoder(MediaType.json)).thenReturn(decoder);

    WebSocketMessageImpl msg = new WebSocketMessageImpl(ctx, "{}".getBytes(StandardCharsets.UTF_8));

    // Test to(Type)
    assertEquals("decoded-result", msg.to(String.class));

    // Test toNullable(Type) which delegates to to(Type)
    assertEquals("decoded-result", msg.toNullable(String.class));
  }

  @Test
  @DisplayName("Test private WebSocketMessageBody methods via reflection for 100% coverage")
  void testWebSocketMessageBody() throws Exception {
    // Because WebSocketMessageBody is private and its body() overloads are
    // rarely invoked internally by Jooby's decode mechanism, we use reflection
    // to instantiate it and explicitly verify those methods for 100% coverage.

    Class<?> innerClass =
        Class.forName("io.jooby.internal.WebSocketMessageImpl$WebSocketMessageBody");
    Constructor<?> constructor = innerClass.getDeclaredConstructors()[0];
    constructor.setAccessible(true);

    Body mockBody = mock(Body.class);
    when(mockBody.to(String.class)).thenReturn("class-result");
    when(mockBody.to((Type) String.class)).thenReturn("type-result");

    // Instantiate WebSocketMessageBody
    Context innerCtx = (Context) constructor.newInstance(ctx, mockBody);

    // Verify overridden Context body() methods
    assertSame(mockBody, innerCtx.body());
    assertEquals("class-result", innerCtx.body(String.class));
    assertEquals("type-result", innerCtx.body((Type) String.class));
  }
}
