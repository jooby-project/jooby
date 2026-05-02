/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Sender;
import io.jooby.output.Output;

@ExtendWith(MockitoExtension.class)
class JettySenderTest {

  @Mock JettyContext ctx;
  @Mock Response response;
  @Mock Sender.Callback joobyCallback;

  private JettySender sender;

  @BeforeEach
  void setup() {
    sender = new JettySender(ctx, response);
  }

  @Test
  void testWriteByteArray() {
    byte[] data = {1, 2, 3, 4, 5};

    Sender result = sender.write(data, joobyCallback);

    assertSame(sender, result);

    ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
    ArgumentCaptor<org.eclipse.jetty.util.Callback> callbackCaptor =
        ArgumentCaptor.forClass(org.eclipse.jetty.util.Callback.class);

    verify(response).write(eq(false), bufferCaptor.capture(), callbackCaptor.capture());

    // Verify buffer was wrapped correctly
    ByteBuffer capturedBuffer = bufferCaptor.getValue();
    byte[] capturedData = new byte[capturedBuffer.remaining()];
    capturedBuffer.get(capturedData);
    assertArrayEquals(data, capturedData);

    // Verify Jetty Callback success bridge
    org.eclipse.jetty.util.Callback jettyCallback = callbackCaptor.getValue();
    jettyCallback.succeeded();
    verify(joobyCallback).onComplete(ctx, null);

    // Verify Jetty Callback failure bridge
    Throwable error = new RuntimeException("Write failed");
    jettyCallback.failed(error);
    verify(joobyCallback).onComplete(ctx, error);
  }

  @Test
  void testWriteOutput() {
    Output output = mock(Output.class);
    JettyCallbacks.OutputCallback outputCallback = mock(JettyCallbacks.OutputCallback.class);

    try (MockedStatic<JettyCallbacks> callbacksStatic = mockStatic(JettyCallbacks.class)) {
      callbacksStatic
          .when(
              () ->
                  JettyCallbacks.fromOutput(
                      eq(response), any(org.eclipse.jetty.util.Callback.class), eq(output)))
          .thenReturn(outputCallback);

      Sender result = sender.write(output, joobyCallback);

      assertSame(sender, result);

      // Verify that the callback mechanism was triggered with closeOnLast = false
      verify(outputCallback).send(false);
    }
  }

  @Test
  void testClose() {
    sender.close();

    // As per JettySender implementation, ctx is passed as the fallback callback during close
    verify(response).write(false, null, ctx);
  }
}
