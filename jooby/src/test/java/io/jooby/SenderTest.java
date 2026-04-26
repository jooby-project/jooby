/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SenderTest {

  private Sender sender;
  private Sender.Callback callback;

  @BeforeEach
  void setUp() {
    // We use CALLS_REAL_METHODS to test the default logic in the interface
    sender = mock(Sender.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    callback = mock(Sender.Callback.class);
  }

  @Test
  void writeStringWithDefaultCharset() {
    String data = "hello";
    byte[] expectedBytes = data.getBytes(StandardCharsets.UTF_8);

    sender.write(data, callback);

    // Verify it delegates to write(String, Charset, Callback)
    // which delegates to write(byte[], Callback)
    verify(sender).write(eq(expectedBytes), eq(callback));
  }

  @Test
  void writeStringWithCustomCharset() {
    String data = "hello";
    var charset = StandardCharsets.UTF_16;
    byte[] expectedBytes = data.getBytes(charset);

    sender.write(data, charset, callback);

    // Verify it delegates to write(byte[], Callback) with correct bytes
    verify(sender).write(eq(expectedBytes), eq(callback));
  }
}
