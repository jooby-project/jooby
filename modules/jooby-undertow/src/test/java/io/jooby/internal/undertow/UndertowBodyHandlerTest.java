/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Body;
import io.jooby.Route;
import io.jooby.Router;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

@ExtendWith(MockitoExtension.class)
class UndertowBodyHandlerTest {

  @Mock Router.Match route;
  @Mock UndertowContext context;
  @Mock HttpServerExchange exchange;
  @Mock Router router;

  @TempDir Path tempDir;

  @BeforeEach
  void setup() {
    lenient().when(context.getRouter()).thenReturn(router);
    lenient().when(router.getTmpdir()).thenReturn(tempDir);
  }

  @Test
  void testHandleFullBytes() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);

    try (MockedStatic<Body> bodyStatic = mockStatic(Body.class)) {
      Body mockBody = mock(Body.class);
      bodyStatic.when(() -> Body.of(eq(context), any(byte[].class))).thenReturn(mockBody);

      handler.handle(exchange, new byte[] {1, 2, 3});

      verify(route).execute(context);
    }
  }

  @Test
  void testExchangeEvent_Success() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);

    Path mockPath = mock(Path.class);
    Field fileField = UndertowBodyHandler.class.getDeclaredField("file");
    fileField.setAccessible(true);
    fileField.set(handler, mockPath);

    ExchangeCompletionListener.NextListener next =
        mock(ExchangeCompletionListener.NextListener.class);

    try (MockedStatic<Files> filesStatic = mockStatic(Files.class)) {
      filesStatic.when(() -> Files.deleteIfExists(mockPath)).thenReturn(true);

      handler.exchangeEvent(exchange, next);

      filesStatic.verify(() -> Files.deleteIfExists(mockPath));
      verify(next).proceed();
    }
  }

  @Test
  void testExchangeEvent_ThrowsIOException() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);

    Path mockPath = mock(Path.class);
    Field fileField = UndertowBodyHandler.class.getDeclaredField("file");
    fileField.setAccessible(true);
    fileField.set(handler, mockPath);

    ExchangeCompletionListener.NextListener next =
        mock(ExchangeCompletionListener.NextListener.class);

    try (MockedStatic<Files> filesStatic = mockStatic(Files.class)) {
      filesStatic
          .when(() -> Files.deleteIfExists(mockPath))
          .thenThrow(new IOException("Delete failed"));

      handler.exchangeEvent(exchange, next);

      verify(next).proceed(); // Must proceed even on exception
    }
  }

  @Test
  void testHandlePartial_EmptyChunk_NotLast() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);
    handler.handle(exchange, new byte[0], false);

    verify(route, never()).execute(any());
  }

  @Test
  void testHandlePartial_EntityTooLarge_WithoutChannel() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 10, 5);
    handler.handle(exchange, new byte[] {1, 2, 3, 4, 5, 6}, false); // 6 > 5 maxRequestSize

    verify(route).execute(context, Route.REQUEST_ENTITY_TOO_LARGE);
  }

  @Test
  void testHandlePartial_EntityTooLarge_WithChannel() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 15);

    FileChannel mockChannel = mock(FileChannel.class);
    try (MockedStatic<FileChannel> fileChannelStatic = mockStatic(FileChannel.class)) {
      fileChannelStatic
          .when(() -> FileChannel.open(any(), eq(CREATE), eq(WRITE)))
          .thenReturn(mockChannel);

      // Spill to file (create channel)
      handler.handle(exchange, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, false);

      // Exceed max size limit
      handler.handle(exchange, new byte[] {11, 12, 13, 14, 15, 16}, false);

      verify(route).execute(context, Route.REQUEST_ENTITY_TOO_LARGE);
      try {
        verify(mockChannel).close(); // Ensure cleanup occurred in finally block
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  void testHandlePartial_InMemory_Last() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 10, 20);

    try (MockedStatic<Body> bodyStatic = mockStatic(Body.class)) {
      Body mockBody = mock(Body.class);
      bodyStatic.when(() -> Body.of(eq(context), any(byte[].class))).thenReturn(mockBody);

      handler.handle(exchange, new byte[] {1, 2}, false); // chunks == null -> creates chunks
      handler.handle(exchange, new byte[] {3, 4}, false); // chunks != null -> appends
      handler.handle(exchange, new byte[] {5}, true); // merges into one array and sets body

      verify(route).execute(context);
    }
  }

  @Test
  void testHandlePartial_EmptyChunk_Last_WithChunks() {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 10, 20);

    try (MockedStatic<Body> bodyStatic = mockStatic(Body.class)) {
      Body mockBody = mock(Body.class);
      bodyStatic.when(() -> Body.of(eq(context), any(byte[].class))).thenReturn(mockBody);

      handler.handle(exchange, new byte[] {1, 2}, false);
      handler.handle(exchange, new byte[0], true); // Gracefully handles empty final chunk

      verify(route).execute(context);
    }
  }

  @Test
  void testHandlePartial_SpillToFile_Directly() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);
    // Directly exceeds buffer size (10 > 5). chunks is null.
    handler.handle(exchange, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, false);

    Field fileField = UndertowBodyHandler.class.getDeclaredField("file");
    fileField.setAccessible(true);
    Path file = (Path) fileField.get(handler);

    assertTrue(Files.exists(file));
  }

  @Test
  void testHandlePartial_SpillToFile_WithChunks_Last() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 10, 20);

    try (MockedStatic<Body> bodyStatic = mockStatic(Body.class)) {
      Body mockBody = mock(Body.class);
      bodyStatic.when(() -> Body.of(eq(context), any(Path.class))).thenReturn(mockBody);

      // Memory chunk
      handler.handle(exchange, new byte[] {1, 2, 3, 4, 5}, false);
      // Spill chunk (pushes memory blocks down to FileChannel)
      handler.handle(exchange, new byte[] {6, 7, 8, 9, 10, 11}, false);
      // Last chunk
      handler.handle(exchange, new byte[] {12}, true);

      verify(exchange).addExchangeCompleteListener(handler);
      verify(route).execute(context);
    }

    Field fileField = UndertowBodyHandler.class.getDeclaredField("file");
    fileField.setAccessible(true);
    Path file = (Path) fileField.get(handler);

    byte[] actual = Files.readAllBytes(file);
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, actual);
  }

  @Test
  void testIOExceptionDuringForceAndClose() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);
    FileChannel mockChannel = mock(FileChannel.class);

    try (MockedStatic<FileChannel> fileChannelStatic = mockStatic(FileChannel.class)) {
      fileChannelStatic
          .when(() -> FileChannel.open(any(), eq(CREATE), eq(WRITE)))
          .thenReturn(mockChannel);

      // Trigger spill to set channel
      handler.handle(exchange, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, false);

      // Mock force to throw (will be caught by the outer loop in handle())
      IOException forceEx = new IOException("Force failed");
      doThrow(forceEx).when(mockChannel).force(true);

      // Mock close to throw to test closeChannel exception suppression
      doThrow(new IOException("Close failed")).when(mockChannel).close();

      // Trigger last chunk to call forceAndClose
      handler.handle(exchange, new byte[] {11}, true);

      verify(mockChannel).force(true);
      // Evaluates once in forceAndClose finally, once in handle catch finally
      verify(mockChannel, times(2)).close();
      verify(context).sendError(forceEx);
      verify(exchange).endExchange();
    }
  }

  @Test
  void testHandlePartial_IOException_DuringWrite() throws Exception {
    UndertowBodyHandler handler = new UndertowBodyHandler(route, context, 5, 20);

    try (MockedStatic<FileChannel> fileChannelStatic = mockStatic(FileChannel.class)) {
      IOException ioEx = new IOException("Disk full");
      fileChannelStatic.when(() -> FileChannel.open(any(), eq(CREATE), eq(WRITE))).thenThrow(ioEx);

      handler.handle(exchange, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, false);

      verify(context).sendError(ioEx);
      verify(exchange).endExchange();
    }
  }
}
