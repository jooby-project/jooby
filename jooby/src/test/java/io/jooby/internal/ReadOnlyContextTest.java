/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.FileDownload;
import io.jooby.MediaType;
import io.jooby.StatusCode;

public class ReadOnlyContextTest {

  private ReadOnlyContext readOnly;
  private Context delegate;

  @BeforeEach
  void setUp() {
    delegate = mock(Context.class);
    readOnly = new ReadOnlyContext(delegate);
  }

  @Test
  void shouldAlwaysReportResponseStarted() {
    assertTrue(readOnly.isResponseStarted());
  }

  @ParameterizedTest(name = "Method {0} should throw IllegalStateException")
  @MethodSource("provideForbiddenMethods")
  void shouldThrowExceptionOnResponseModification(Runnable action) {
    IllegalStateException ex = assertThrows(IllegalStateException.class, action::run);
    assertEquals("The response has already been started", ex.getMessage());
  }

  private static Stream<Arguments> provideForbiddenMethods() {
    Context ctx = mock(Context.class);
    ReadOnlyContext ro = new ReadOnlyContext(ctx);

    return Stream.of(
        // Send variants
        Arguments.of((Runnable) () -> ro.send(Paths.get("file.txt"))),
        Arguments.of((Runnable) () -> ro.send(new byte[0])),
        Arguments.of((Runnable) () -> ro.send("data")),
        Arguments.of((Runnable) () -> ro.send("data", StandardCharsets.UTF_8)),
        Arguments.of((Runnable) () -> ro.send(ByteBuffer.allocate(0))),
        Arguments.of((Runnable) () -> ro.send(mock(FileChannel.class))),
        Arguments.of((Runnable) () -> ro.send(mock(FileDownload.class))),
        Arguments.of((Runnable) () -> ro.send(mock(InputStream.class))),
        Arguments.of((Runnable) () -> ro.send(StatusCode.OK)),
        Arguments.of((Runnable) () -> ro.send(mock(ReadableByteChannel.class))),

        // Error & Redirect
        Arguments.of((Runnable) () -> ro.sendError(new RuntimeException())),
        Arguments.of(
            (Runnable) () -> ro.sendError(new RuntimeException(), StatusCode.SERVER_ERROR)),
        Arguments.of((Runnable) () -> ro.sendRedirect("/loc")),
        Arguments.of((Runnable) () -> ro.sendRedirect(StatusCode.FOUND, "/loc")),

        // Render & Headers
        Arguments.of((Runnable) () -> ro.render(new Object())),
        Arguments.of((Runnable) () -> ro.removeResponseHeader("name")),
        Arguments.of((Runnable) () -> ro.setResponseCookie(mock(Cookie.class))),
        Arguments.of((Runnable) () -> ro.setResponseHeader("n", new Date())),
        Arguments.of((Runnable) () -> ro.setResponseHeader("n", Instant.now())),
        Arguments.of((Runnable) () -> ro.setResponseHeader("n", "v")),
        Arguments.of((Runnable) () -> ro.setResponseHeader("n", new Object())),

        // Status & Type
        Arguments.of((Runnable) () -> ro.setResponseCode(200)),
        Arguments.of((Runnable) () -> ro.setResponseCode(StatusCode.OK)),
        Arguments.of((Runnable) () -> ro.setResponseLength(100L)),
        Arguments.of((Runnable) () -> ro.setResponseType("text/plain")),
        Arguments.of((Runnable) () -> ro.setResponseType(MediaType.text)),
        Arguments.of((Runnable) () -> ro.setDefaultResponseType(MediaType.text)),

        // Streams & Writers
        Arguments.of((Runnable) () -> ro.responseStream()),
        Arguments.of((Runnable) () -> ro.responseStream(MediaType.json)),
        Arguments.of((Runnable) () -> ro.responseWriter()),
        Arguments.of((Runnable) () -> ro.responseWriter(MediaType.text)),
        Arguments.of((Runnable) () -> ro.responseSender()));
  }

  @Test
  void shouldThrowOnFunctionalStreams() {
    // These methods throw checked exceptions, so we handle them separately from the Runnable stream
    assertThrows(IllegalStateException.class, () -> readOnly.responseStream(out -> {}));
    assertThrows(
        IllegalStateException.class, () -> readOnly.responseStream(MediaType.json, out -> {}));
    assertThrows(IllegalStateException.class, () -> readOnly.responseWriter(writer -> {}));
    assertThrows(
        IllegalStateException.class, () -> readOnly.responseWriter(MediaType.text, writer -> {}));
  }
}
