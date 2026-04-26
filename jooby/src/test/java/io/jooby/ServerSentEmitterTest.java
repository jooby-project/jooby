/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ServerSentEmitterTest {

  private ServerSentEmitter emitter;
  private Context ctx;

  @BeforeEach
  void setUp() {
    emitter = mock(ServerSentEmitter.class);
    ctx = mock(Context.class);

    // Wire up the mock to execute the default interface methods
    when(emitter.getContext()).thenReturn(ctx);
    when(emitter.getAttributes()).thenCallRealMethod();
    when(emitter.attribute(anyString())).thenCallRealMethod();
    when(emitter.attribute(anyString(), any())).thenCallRealMethod();
    when(emitter.send(anyString())).thenCallRealMethod();
    when(emitter.send(any(byte[].class))).thenCallRealMethod();
    when(emitter.send(any(Object.class))).thenCallRealMethod();
    when(emitter.send(anyString(), any())).thenCallRealMethod();
    when(emitter.keepAlive(anyLong(), any(TimeUnit.class))).thenCallRealMethod();
    when(emitter.getLastEventId()).thenCallRealMethod();
    when(emitter.lastEventId(any())).thenCallRealMethod();
  }

  @Test
  void testKeepAliveTaskSuccess() {
    when(emitter.isOpen()).thenReturn(true);
    when(emitter.getId()).thenReturn("sse-123");

    ServerSentEmitter.KeepAlive keepAlive = new ServerSentEmitter.KeepAlive(emitter, 5000L);
    keepAlive.run();

    verify(emitter).send(":sse-123\n");
    verify(emitter).keepAlive(5000L);
  }

  @Test
  void testKeepAliveTaskError() {
    when(emitter.isOpen()).thenReturn(true);
    when(emitter.getId()).thenReturn("sse-123");
    doThrow(new RuntimeException("Link dead")).when(emitter).send(anyString());

    ServerSentEmitter.KeepAlive keepAlive = new ServerSentEmitter.KeepAlive(emitter, 5000L);
    keepAlive.run();

    verify(emitter).close();
  }

  @Test
  void testKeepAliveTaskWhenClosed() {
    when(emitter.isOpen()).thenReturn(false);

    ServerSentEmitter.KeepAlive keepAlive = new ServerSentEmitter.KeepAlive(emitter, 5000L);
    keepAlive.run();

    verify(emitter, never()).send(anyString());
  }

  @Test
  void testAttributeMethods() {
    Map<String, Object> attrs = Map.of("k", "v");
    when(ctx.getAttributes()).thenReturn(attrs);
    when(ctx.getAttribute("k")).thenReturn("v");

    // 1. Test getAttributes()
    assertEquals(attrs, emitter.getAttributes());

    // 2. Test attribute(key)
    assertEquals("v", emitter.attribute("k"));

    // 3. Test attribute(key, value)
    // We do NOT stub this; we let thenCallRealMethod() from setUp run it
    ServerSentEmitter result = emitter.attribute("name", "jooby");

    assertEquals(emitter, result);
    verify(ctx).setAttribute("name", "jooby");
  }

  @Test
  void testSendDefaultMethods() {
    // 1. String send
    emitter.send("hello");
    verify(emitter)
        .send(
            (ServerSentMessage)
                argThat(
                    msg ->
                        msg instanceof ServerSentMessage sseMsg
                            && "hello".equals(sseMsg.getData())));

    // 2. Byte array send
    byte[] bytes = new byte[] {1, 2};
    emitter.send(bytes);
    verify(emitter)
        .send(
            (ServerSentMessage)
                argThat(
                    msg -> msg instanceof ServerSentMessage sseMsg && sseMsg.getData() == bytes));

    // 3. Object send (non-SSE message)
    emitter.send((Object) 123);
    verify(emitter)
        .send(
            (ServerSentMessage)
                argThat(
                    msg ->
                        msg instanceof ServerSentMessage sseMsg
                            && Integer.valueOf(123).equals(sseMsg.getData())));

    // 4. Object send (is-SSE message)
    ServerSentMessage sseMsg = new ServerSentMessage("data");
    emitter.send((Object) sseMsg);
    verify(emitter).send(sseMsg);

    // 5. Event + Data send
    emitter.send("update", "payload");
    verify(emitter)
        .send(
            (ServerSentMessage)
                argThat(
                    msg ->
                        msg instanceof ServerSentMessage sseMsg1
                            && "payload".equals(sseMsg1.getData())
                            && "update".equals(sseMsg1.getEvent())));
  }

  @Test
  void testKeepAliveWithUnit() {
    emitter.keepAlive(1, TimeUnit.SECONDS);
    verify(emitter).keepAlive(1000L);
  }

  @Test
  void testLastEventId() {
    // Header present
    when(ctx.header("Last-Event-ID"))
        .thenReturn(Value.value(new ValueFactory(), "Last-Event-ID", "100"));
    assertEquals("100", emitter.getLastEventId());
    assertEquals(100, (Integer) emitter.lastEventId(Integer.class));

    // Header missing
    when(ctx.header("Last-Event-ID"))
        .thenReturn(Value.missing(new ValueFactory(), "Last-Event-ID"));
    assertNull(emitter.getLastEventId());
  }
}
