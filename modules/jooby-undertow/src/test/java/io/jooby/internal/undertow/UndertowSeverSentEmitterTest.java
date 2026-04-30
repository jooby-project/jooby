/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.xnio.XnioIoThread;

import io.jooby.Context;
import io.jooby.Server;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;
import io.undertow.server.HttpServerExchange;

@ExtendWith(MockitoExtension.class)
class UndertowSeverSentEmitterTest {

  @Mock UndertowContext context;
  @Mock HttpServerExchange exchange;
  @Mock Logger logger;

  @BeforeEach
  void setup() {
    context.exchange = exchange;
    // FIX: Stub the path so the emitter can read it without throwing null,
    // and we can verify it cleanly without inline mock calls.
    lenient().when(context.getRequestPath()).thenReturn("/sse-path");
  }

  private UndertowSeverSentEmitter createEmitter() throws Exception {
    UndertowSeverSentEmitter emitter = new UndertowSeverSentEmitter(context);

    // Inject mock logger via reflection
    Field logField = UndertowSeverSentEmitter.class.getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(emitter, logger);

    return emitter;
  }

  @Test
  void testIdAndIsOpen() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();

      assertNotNull(emitter.getId());
      assertTrue(emitter.isOpen());

      emitter.setId("custom-id");
      assertEquals("custom-id", emitter.getId());
    }
  }

  @Test
  void testGetContext() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
            mockConstruction(UndertowServerSentConnection.class);
        MockedStatic<Context> contextStatic = mockStatic(Context.class)) {

      UndertowSeverSentEmitter emitter = createEmitter();
      Context readOnlyContext = mock(Context.class);
      contextStatic.when(() -> Context.readOnly(context)).thenReturn(readOnlyContext);

      assertSame(readOnlyContext, emitter.getContext());
    }
  }

  @Test
  void testSendOpen() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      UndertowServerSentConnection connection = mocked.constructed().get(0);
      when(exchange.isComplete()).thenReturn(false);

      ServerSentMessage message = mock(ServerSentMessage.class);
      emitter.send(message);

      verify(connection).send(message, null);
    }
  }

  @Test
  void testSendWhenExchangeIsComplete() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      when(exchange.isComplete()).thenReturn(true);

      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      emitter.onClose(task);

      ServerSentMessage message = mock(ServerSentMessage.class);
      emitter.send(message);

      // Verify checkOpen returning false triggered close() and the log statement
      verify(task).run();
      verify(mocked.constructed().get(0)).close();
      verify(logger).debug("server-sent-event closed: {}", emitter.getId());
      assertFalse(emitter.isOpen());
    }
  }

  @Test
  void testSendWhenAlreadyClosed() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();

      emitter.close();
      assertFalse(emitter.isOpen());

      ServerSentMessage message = mock(ServerSentMessage.class);
      emitter.send(message);

      verify(logger).debug("server-sent-event closed: {}", emitter.getId());
      verify(mocked.constructed().get(0), never()).send(any(), any());
    }
  }

  @Test
  void testKeepAliveOpen() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      when(exchange.isComplete()).thenReturn(false);

      XnioIoThread ioThread = mock(XnioIoThread.class);
      when(exchange.getIoThread()).thenReturn(ioThread);

      emitter.keepAlive(1500L);

      verify(ioThread).executeAfter(any(Runnable.class), eq(1500L), eq(TimeUnit.MILLISECONDS));
    }
  }

  @Test
  void testKeepAliveClosed() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      when(exchange.isComplete()).thenReturn(true);

      emitter.keepAlive(1000L);

      // Because checkOpen returns false, it should NOT access getIoThread
      verify(exchange, never()).getIoThread();
    }
  }

  @Test
  void testCloseNoTask() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();

      emitter.close();

      assertFalse(emitter.isOpen());
      // Due to the code's branch logic, connection.close() is NOT called when closeTask is null
      verify(mocked.constructed().get(0), never()).close();
    }
  }

  @Test
  void testCloseWithTaskSuccess() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      emitter.onClose(task);

      emitter.close();

      verify(task).run();
      verify(mocked.constructed().get(0)).close();
      assertFalse(emitter.isOpen());
    }
  }

  @Test
  void testCloseCalledTwice() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      emitter.onClose(task);

      emitter.close();
      emitter.close(); // Second call should abort fast due to compareAndSet

      verify(task, times(1)).run();
    }
  }

  @Test
  void testCloseWithTaskThrowsException() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      doThrow(new RuntimeException("Task Failed")).when(task).run();
      emitter.onClose(task);

      assertThrows(RuntimeException.class, () -> emitter.close());

      // Connection should still be closed due to the finally block
      verify(mocked.constructed().get(0)).close();
      assertFalse(emitter.isOpen());
    }
  }

  @Test
  void testCloseWithTaskConnectionThrowsIOException() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      emitter.onClose(task);

      UndertowServerSentConnection connection = mocked.constructed().get(0);
      IOException ioException = new IOException("Connection Error");
      doThrow(ioException).when(connection).close();

      emitter.close();

      verify(task).run();
      verify(logger)
          .error(
              eq("server-sent-event resulted in exception: id {} {}"),
              eq(emitter.getId()),
              eq("/sse-path"), // FIX: Use explicit hardcoded string
              eq(ioException));
    }
  }

  @Test
  void testDone() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
        mockConstruction(UndertowServerSentConnection.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      UndertowServerSentConnection connection = mocked.constructed().get(0);
      ServerSentMessage message = mock(ServerSentMessage.class);

      when(message.getId()).thenReturn("msg-id");
      when(message.getEvent()).thenReturn("msg-event");
      when(message.getData()).thenReturn("msg-data");

      emitter.done(connection, message);

      verify(logger)
          .debug(
              "server-sent-event {} message sent id: {}, event: {}, data: {}",
              emitter.getId(),
              "msg-id",
              "msg-event",
              "msg-data");
    }
  }

  @Test
  void testFailedConnectionLost() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
            mockConstruction(UndertowServerSentConnection.class);
        MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      UndertowServerSentConnection connection = mocked.constructed().get(0);
      ServerSentMessage message = mock(ServerSentMessage.class);
      Throwable exception = new RuntimeException("Connection Dropped");

      serverStatic.when(() -> Server.connectionLost(exception)).thenReturn(true);

      SneakyThrows.Runnable task = mock(SneakyThrows.Runnable.class);
      emitter.onClose(task);

      emitter.failed(connection, message, exception);

      verify(task).run();
      assertFalse(emitter.isOpen());
    }
  }

  @Test
  void testFailedNotFatal() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
            mockConstruction(UndertowServerSentConnection.class);
        MockedStatic<Server> serverStatic = mockStatic(Server.class);
        MockedStatic<SneakyThrows> sneakyThrowsStatic = mockStatic(SneakyThrows.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      UndertowServerSentConnection connection = mocked.constructed().get(0);
      ServerSentMessage message = mock(ServerSentMessage.class);
      Throwable exception = new RuntimeException("Generic Error");

      serverStatic.when(() -> Server.connectionLost(exception)).thenReturn(false);
      sneakyThrowsStatic.when(() -> SneakyThrows.isFatal(exception)).thenReturn(false);

      emitter.failed(connection, message, exception);

      verify(logger)
          .error(
              eq("server-sent-event resulted in exception: id {} {}"),
              eq(emitter.getId()),
              eq("/sse-path"), // FIX: Use explicit hardcoded string
              eq(exception));
      sneakyThrowsStatic.verify(() -> SneakyThrows.propagate(any()), never());
    }
  }

  @Test
  void testFailedFatal() throws Exception {
    try (MockedConstruction<UndertowServerSentConnection> mocked =
            mockConstruction(UndertowServerSentConnection.class);
        MockedStatic<Server> serverStatic = mockStatic(Server.class);
        MockedStatic<SneakyThrows> sneakyThrowsStatic = mockStatic(SneakyThrows.class)) {
      UndertowSeverSentEmitter emitter = createEmitter();
      UndertowServerSentConnection connection = mocked.constructed().get(0);
      ServerSentMessage message = mock(ServerSentMessage.class);
      Throwable exception = new OutOfMemoryError("Fatal Error");
      RuntimeException propagated = new RuntimeException("Propagated");

      serverStatic.when(() -> Server.connectionLost(exception)).thenReturn(false);
      sneakyThrowsStatic.when(() -> SneakyThrows.isFatal(exception)).thenReturn(true);
      sneakyThrowsStatic.when(() -> SneakyThrows.propagate(exception)).thenReturn(propagated);

      assertThrows(RuntimeException.class, () -> emitter.failed(connection, message, exception));

      verify(logger)
          .error(
              eq("server-sent-event resulted in exception: id {} {}"),
              eq(emitter.getId()),
              eq("/sse-path"), // FIX: Use explicit hardcoded string
              eq(exception));
    }
  }
}
