/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Sender;
import io.jooby.output.Output;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;

@ExtendWith(MockitoExtension.class)
class UndertowSenderTest {

  @Mock UndertowContext ctx;
  @Mock HttpServerExchange exchange;
  @Mock io.undertow.io.Sender responseSender;
  @Mock Sender.Callback callback;

  private UndertowSender sender;

  @BeforeEach
  void setup() {
    sender = new UndertowSender(ctx, exchange);
  }

  @Test
  void testWriteByteArray() {
    when(exchange.getResponseSender()).thenReturn(responseSender);

    byte[] data = {1, 2, 3, 4, 5};
    sender.write(data, callback);

    ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
    ArgumentCaptor<IoCallback> callbackCaptor = ArgumentCaptor.forClass(IoCallback.class);

    verify(responseSender).send(bufferCaptor.capture(), callbackCaptor.capture());

    // Verify buffer was wrapped correctly
    ByteBuffer capturedBuffer = bufferCaptor.getValue();
    byte[] capturedData = new byte[capturedBuffer.remaining()];
    capturedBuffer.get(capturedData);
    assertArrayEquals(data, capturedData);
  }

  @Test
  void testWriteOutput() {
    Output output = mock(Output.class);

    // Intercept the creation of the UndertowOutputCallback to ensure it is instantiated and sent
    try (MockedConstruction<UndertowOutputCallback> mockedCallback =
        mockConstruction(UndertowOutputCallback.class)) {

      sender.write(output, callback);

      assertEquals(1, mockedCallback.constructed().size());
      UndertowOutputCallback constructedCallback = mockedCallback.constructed().get(0);

      verify(constructedCallback).send(exchange);
    }
  }

  @Test
  void testClose() {
    sender.close();
    verify(ctx).destroy(null);
  }

  @Test
  void testIoCallback_OnComplete() {
    when(exchange.getResponseSender()).thenReturn(responseSender);

    sender.write(new byte[0], callback);

    // Capture the internally generated IoCallback
    ArgumentCaptor<IoCallback> captor = ArgumentCaptor.forClass(IoCallback.class);
    verify(responseSender).send(any(ByteBuffer.class), captor.capture());
    IoCallback ioCallback = captor.getValue();

    // Trigger the success callback
    ioCallback.onComplete(exchange, responseSender);

    verify(callback).onComplete(ctx, null);
  }

  @Test
  void testIoCallback_OnException() {
    when(exchange.getResponseSender()).thenReturn(responseSender);

    sender.write(new byte[0], callback);

    // Capture the internally generated IoCallback
    ArgumentCaptor<IoCallback> captor = ArgumentCaptor.forClass(IoCallback.class);
    verify(responseSender).send(any(ByteBuffer.class), captor.capture());
    IoCallback ioCallback = captor.getValue();

    IOException exception = new IOException("Network failure");

    // Trigger the error callback
    ioCallback.onException(exchange, responseSender, exception);

    verify(callback).onComplete(ctx, exception);
    verify(ctx).destroy(exception);
  }

  @Test
  void testIoCallback_OnException_WithCallbackCrash_TriggersFinallyBlock() {
    when(exchange.getResponseSender()).thenReturn(responseSender);

    sender.write(new byte[0], callback);

    ArgumentCaptor<IoCallback> captor = ArgumentCaptor.forClass(IoCallback.class);
    verify(responseSender).send(any(ByteBuffer.class), captor.capture());
    IoCallback ioCallback = captor.getValue();

    IOException exception = new IOException("Network failure");

    // Simulate a crash occurring inside the user-provided callback
    doThrow(new RuntimeException("User callback crashed"))
        .when(callback)
        .onComplete(ctx, exception);

    // Ensure the exception bubbles up
    assertThrows(
        RuntimeException.class, () -> ioCallback.onException(exchange, responseSender, exception));

    // Crucially, verify the 'finally' block still executed ctx.destroy()
    verify(ctx).destroy(exception);
  }
}
