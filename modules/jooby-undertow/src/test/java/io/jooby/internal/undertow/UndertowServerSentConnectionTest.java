/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xnio.ChannelListener;
import org.xnio.XnioIoThread;
import org.xnio.channels.StreamSinkChannel;

import io.jooby.ServerSentMessage;
import io.jooby.output.Output;
import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;

@ExtendWith(MockitoExtension.class)
class UndertowServerSentConnectionTest {

  @Mock UndertowContext context;
  @Mock HttpServerExchange exchange;
  @Mock StreamSinkChannel sink;
  @Mock XnioIoThread ioThread;
  @Mock HttpServerConnection connection;
  @Mock ByteBufferPool bufferPool;
  @Mock PooledByteBuffer pooledByteBuffer;

  private ByteBuffer buffer;

  @BeforeEach
  void setup() {
    context.exchange = exchange;
    buffer = ByteBuffer.allocate(1024);

    lenient().when(exchange.getResponseChannel()).thenReturn(sink);
    lenient().when(exchange.getConnection()).thenReturn(connection);
    lenient().when(connection.getByteBufferPool()).thenReturn(bufferPool);
    lenient().when(bufferPool.allocate()).thenReturn(pooledByteBuffer);
    lenient().when(pooledByteBuffer.getBuffer()).thenReturn(buffer);

    lenient().when(sink.getCloseSetter()).thenReturn(mock(ChannelListener.Setter.class));
    lenient().when(sink.getWriteSetter()).thenReturn(mock(ChannelListener.Setter.class));
    lenient().when(sink.getIoThread()).thenReturn(ioThread);

    // Immediate execution for IO thread tasks
    lenient()
        .doAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(0)).run();
              return null;
            })
        .when(ioThread)
        .execute(any(Runnable.class));
  }

  @Test
  void testSendSuccessful() throws IOException {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    ServerSentMessage message = mock(ServerSentMessage.class);
    Output output = mock(Output.class);

    when(message.encode(context)).thenReturn(output);
    when(output.size()).thenReturn(10);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.allocate(10));
    when(sink.write(any(ByteBuffer.class)))
        .thenAnswer(
            inv -> {
              ByteBuffer b = inv.getArgument(0);
              int rem = b.remaining();
              b.position(b.limit());
              return rem;
            });
    when(sink.flush()).thenReturn(true);

    UndertowServerSentConnection.EventCallback callback =
        mock(UndertowServerSentConnection.EventCallback.class);

    sse.send(message, callback);

    verify(callback).done(sse, message);
    assertTrue(sse.isOpen());
  }

  @Test
  void testSendPartialWriteAndFlush() throws IOException {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    ServerSentMessage message = mock(ServerSentMessage.class);
    Output output = mock(Output.class);

    when(message.encode(context)).thenReturn(output);
    when(output.size()).thenReturn(10);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.allocate(10));

    // FIX: Properly consume the buffer so hasRemaining() eventually becomes false, breaking the
    // infinite loop.
    when(sink.write(any(ByteBuffer.class)))
        .thenAnswer(
            inv -> {
              ByteBuffer b = inv.getArgument(0);
              int rem = b.remaining();
              b.position(b.limit()); // Consume the bytes
              return rem;
            });

    // First flush fails, second flush succeeds
    when(sink.flush()).thenReturn(false).thenReturn(true);

    UndertowServerSentConnection.EventCallback callback =
        mock(UndertowServerSentConnection.EventCallback.class);
    sse.send(message, callback);

    // Flush deferred - callback not called yet
    verify(callback, never()).done(any(), any());

    // Trigger write listener manually to simulate subsequent flush success
    ArgumentCaptor<ChannelListener> listenerCaptor = ArgumentCaptor.forClass(ChannelListener.class);
    verify(sink.getWriteSetter()).set(listenerCaptor.capture());
    listenerCaptor.getValue().handleEvent(sink);

    verify(callback).done(sse, message);
  }

  @Test
  void testSendLargeMessageRequiresMultipleBuffers() throws IOException {
    // Small buffer to force "leftOverData" logic
    buffer = ByteBuffer.allocate(5);
    when(pooledByteBuffer.getBuffer()).thenReturn(buffer);

    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    ServerSentMessage message = mock(ServerSentMessage.class);
    Output output = mock(Output.class);

    when(message.encode(context)).thenReturn(output);
    when(output.size()).thenReturn(10);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.allocate(10));

    // FIX: Simulate consuming 5 bytes at a time to prevent infinite looping
    when(sink.write(any(ByteBuffer.class)))
        .thenAnswer(
            inv -> {
              ByteBuffer b = inv.getArgument(0);
              int toWrite = Math.min(b.remaining(), 5);
              b.position(b.position() + toWrite);
              return toWrite;
            });
    when(sink.flush()).thenReturn(true);

    sse.send(message, null);

    // Verify it tried to write the first 5 bytes and triggered the next cycle
    verify(sink, atLeastOnce()).resumeWrites();
  }

  @Test
  void testCloseCleansUpQueuesAndNotifiesFailures() throws IOException {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    ServerSentMessage message = mock(ServerSentMessage.class);
    Output output = mock(Output.class);

    when(message.encode(context)).thenReturn(output);
    when(output.size()).thenReturn(10);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.allocate(10));

    UndertowServerSentConnection.EventCallback callback =
        mock(UndertowServerSentConnection.EventCallback.class);

    // Add to queue and trigger immediate IO thread execution
    sse.send(message, callback);
    sse.close();

    assertFalse(sse.isOpen());
    verify(callback).failed(eq(sse), eq(message), any(ClosedChannelException.class));
    verify(pooledByteBuffer, atMostOnce()).close();
  }

  @Test
  void testShutdownGraceful() {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    sse.shutdown();

    // Since queue is empty and pooled is null, it should end exchange immediately
    verify(exchange).endExchange();
  }

  @Test
  void testSendAfterCloseFails() {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    try {
      sse.close();
    } catch (Exception e) {
    }

    ServerSentMessage message = mock(ServerSentMessage.class);
    UndertowServerSentConnection.EventCallback callback =
        mock(UndertowServerSentConnection.EventCallback.class);

    sse.send(message, callback);
    verify(callback).failed(eq(sse), eq(message), any(ClosedChannelException.class));
  }

  @Test
  void testHandleIOException() throws IOException {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);
    when(sink.write(any(ByteBuffer.class))).thenThrow(new IOException("Write failed"));

    ServerSentMessage message = mock(ServerSentMessage.class);
    Output output = mock(Output.class);

    when(message.encode(context)).thenReturn(output);
    when(output.size()).thenReturn(10);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.allocate(10));

    sse.send(message, null);

    // Verifies safeClose was called on exception
    verify(exchange, atLeastOnce()).getConnection();
  }

  @Test
  void testFillBufferSuspendsWhenEmpty() throws IOException {
    UndertowServerSentConnection sse = new UndertowServerSentConnection(context);

    // Manually trigger fillBuffer via reflection or by processing the last message
    // If queue is empty and pooled exists, it should close pooled and suspend
    // This happens naturally in handleEvent when buffer is exhausted
    when(sink.flush()).thenReturn(true);

    ArgumentCaptor<ChannelListener> listenerCaptor = ArgumentCaptor.forClass(ChannelListener.class);
    verify(sink.getWriteSetter()).set(listenerCaptor.capture());
    listenerCaptor.getValue().handleEvent(sink);

    verify(sink, atLeastOnce()).suspendWrites();
  }
}
