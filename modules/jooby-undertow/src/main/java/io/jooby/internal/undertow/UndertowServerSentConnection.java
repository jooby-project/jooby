/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.xnio.ChannelExceptionHandler;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ServerSentMessage;
import io.jooby.output.BufferedOutput;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;

/**
 * Represents the server side of a Server Sent Events connection.
 *
 * <p>The class implements Attachable, which provides access to the underlying exchanges
 * attachments.
 *
 * <p>Copy/adapted from {@link io.undertow.server.handlers.sse.ServerSentEventConnection}
 *
 * @author Stuart Douglas
 */
public class UndertowServerSentConnection implements Channel {

  private final HttpServerExchange exchange;
  private final StreamSinkChannel sink;
  private final UndertowServerSentConnection.SseWriteListener writeListener =
      new UndertowServerSentConnection.SseWriteListener();

  private final Context context;

  private PooledByteBuffer pooled;

  private final Deque<UndertowServerSentConnection.SSEData> queue = new ConcurrentLinkedDeque<>();
  private final Queue<UndertowServerSentConnection.SSEData> buffered =
      new ConcurrentLinkedDeque<>();

  /** Messages that have been written to the channel but flush() has failed */
  private final Queue<UndertowServerSentConnection.SSEData> flushingMessages = new ArrayDeque<>();

  private final List<ChannelListener<UndertowServerSentConnection>> closeTasks =
      new CopyOnWriteArrayList<>();
  private static final AtomicIntegerFieldUpdater<UndertowServerSentConnection> openUpdater =
      newUpdater(UndertowServerSentConnection.class, "open");
  private volatile int open = 1;
  private volatile boolean shutdown = false;

  public UndertowServerSentConnection(UndertowContext context) {
    this.context = context;
    this.exchange = context.exchange;
    this.sink = this.exchange.getResponseChannel();
    this.sink
        .getCloseSetter()
        .set(
            (ChannelListener<StreamSinkChannel>)
                channel -> {
                  for (ChannelListener<UndertowServerSentConnection> listener : closeTasks) {
                    ChannelListeners.invokeChannelListener(
                        UndertowServerSentConnection.this, listener);
                  }
                  IoUtils.safeClose(UndertowServerSentConnection.this);
                });
    this.sink.getWriteSetter().set(writeListener);
  }

  /**
   * Sends an event to the remote client
   *
   * @param callback A callback that is notified on Success or failure
   */
  public synchronized void send(
      ServerSentMessage message, UndertowServerSentConnection.EventCallback callback) {
    if (open == 0 || shutdown) {
      if (callback != null) {
        callback.failed(this, message, new ClosedChannelException());
      }
      return;
    }
    queue.add(new UndertowServerSentConnection.SSEData(message, callback));
    sink.getIoThread()
        .execute(
            () -> {
              synchronized (UndertowServerSentConnection.this) {
                if (pooled == null) {
                  fillBuffer();
                  writeListener.handleEvent(sink);
                }
              }
            });
  }

  private void fillBuffer() {
    if (queue.isEmpty()) {
      if (pooled != null) {
        pooled.close();
        pooled = null;
        sink.suspendWrites();
      }
      return;
    }

    if (pooled == null) {
      pooled = exchange.getConnection().getByteBufferPool().allocate();
    } else {
      pooled.getBuffer().clear();
    }
    var buffer = pooled.getBuffer();

    while (!queue.isEmpty() && buffer.hasRemaining()) {
      var data = queue.poll();
      buffered.add(data);
      if (data.leftOverData == null) {
        var message = data.message.encode(context);
        if (message.size() < buffer.remaining()) {
          transferTo(message, buffer);
          buffer.position(buffer.position() + message.size());
          data.endBufferPosition = buffer.position();
        } else {
          queue.addFirst(data);
          int rem = buffer.remaining();
          transferTo(message, 0, buffer, buffer.position(), rem);
          buffer.position(buffer.position() + rem);
          data.leftOverData = message;
          data.leftOverDataOffset = rem;
        }
      } else {
        int remainingData = data.leftOverData.size() - data.leftOverDataOffset;
        if (remainingData > buffer.remaining()) {
          queue.addFirst(data);
          int toWrite = buffer.remaining();
          transferTo(
              data.leftOverData, data.leftOverDataOffset, buffer, buffer.position(), toWrite);
          buffer.position(buffer.position() + toWrite);
          data.leftOverDataOffset += toWrite;
        } else {
          transferTo(
              data.leftOverData, data.leftOverDataOffset, buffer, buffer.position(), remainingData);
          buffer.position(buffer.position() + remainingData);
          data.endBufferPosition = buffer.position();
          data.leftOverData = null;
        }
      }
    }
    buffer.flip();
    sink.resumeWrites();
  }

  /**
   * Copy bytes into the given buffer.
   *
   * @param dest Destination buffer.
   */
  private void transferTo(@NonNull BufferedOutput source, @NonNull ByteBuffer dest) {
    transferTo(source, 0, dest, dest.position(), source.size());
  }

  /**
   * Copies the given length from this data buffer into the given destination {@code ByteBuffer},
   * beginning at the given source position, and the given destination position in the destination
   * byte buffer.
   *
   * @param srcPos the position of this data buffer from where copying should start
   * @param dest the destination byte buffer
   * @param destPos the position in {@code dest} to where copying should start
   * @param length the amount of data to copy
   */
  private void transferTo(
      @NonNull BufferedOutput source,
      int srcPos,
      @NonNull ByteBuffer dest,
      int destPos,
      int length) {
    dest = dest.duplicate().clear();
    dest.put(destPos, source.asByteBuffer(), srcPos, length);
  }

  /** execute a graceful shutdown once all data has been sent */
  public void shutdown() {
    if (open == 0 || shutdown) {
      return;
    }
    shutdown = true;
    sink.getIoThread()
        .execute(
            () -> {
              synchronized (UndertowServerSentConnection.this) {
                if (queue.isEmpty() && pooled == null) {
                  exchange.endExchange();
                }
              }
            });
  }

  @Override
  public boolean isOpen() {
    return open != 0;
  }

  @Override
  public void close() throws IOException {
    close(new ClosedChannelException());
  }

  private synchronized void close(IOException e) throws IOException {
    if (openUpdater.compareAndSet(this, 1, 0)) {
      if (pooled != null) {
        pooled.close();
        pooled = null;
      }
      List<UndertowServerSentConnection.SSEData> cb =
          new ArrayList<>(buffered.size() + queue.size() + flushingMessages.size());
      cb.addAll(buffered);
      cb.addAll(queue);
      cb.addAll(flushingMessages);
      queue.clear();
      buffered.clear();
      flushingMessages.clear();
      for (UndertowServerSentConnection.SSEData i : cb) {
        if (i.callback != null) {
          i.callback.failed(this, i.message, e);
        }
      }
      sink.shutdownWrites();
      if (!sink.flush()) {
        sink.getWriteSetter()
            .set(
                ChannelListeners.flushingChannelListener(
                    null,
                    (ChannelExceptionHandler<StreamSinkChannel>)
                        (channel, exception) -> IoUtils.safeClose(sink)));
        sink.resumeWrites();
      }
    }
  }

  public interface EventCallback {

    /**
     * Notification that is called when a message is sucessfully sent
     *
     * @param connection The connection
     * @param message The message event
     */
    void done(UndertowServerSentConnection connection, ServerSentMessage message);

    /**
     * Notification that is called when a message send fails.
     *
     * @param connection The connection
     * @param message The message event
     * @param cause The exception
     */
    void failed(
        UndertowServerSentConnection connection, ServerSentMessage message, Throwable cause);
  }

  private static class SSEData {
    final ServerSentMessage message;
    final UndertowServerSentConnection.EventCallback callback;
    private int endBufferPosition = -1;
    private BufferedOutput leftOverData;
    private int leftOverDataOffset;

    private SSEData(
        ServerSentMessage message, UndertowServerSentConnection.EventCallback callback) {
      this.message = message;
      this.callback = callback;
    }

    @Override
    public String toString() {
      return "{"
          + "message="
          + message
          + ", callback="
          + callback
          + ", endBufferPosition="
          + endBufferPosition
          + ", leftOverDataOffset="
          + leftOverDataOffset
          + '}';
    }
  }

  private class SseWriteListener implements ChannelListener<StreamSinkChannel> {
    @Override
    public void handleEvent(StreamSinkChannel channel) {
      synchronized (UndertowServerSentConnection.this) {
        try {
          if (!flushingMessages.isEmpty()) {
            if (!channel.flush()) {
              return;
            }
            for (UndertowServerSentConnection.SSEData data : flushingMessages) {
              if (data.callback != null && data.leftOverData == null) {
                data.callback.done(UndertowServerSentConnection.this, data.message);
              }
            }
            flushingMessages.clear();
            ByteBuffer buffer = pooled.getBuffer();
            if (!buffer.hasRemaining()) {
              fillBuffer();
              if (pooled == null) {
                if (channel.flush()) {
                  channel.suspendWrites();
                }
                return;
              }
            }
          } else if (pooled == null) {
            if (channel.flush()) {
              channel.suspendWrites();
            }
            return;
          }

          ByteBuffer buffer = pooled.getBuffer();
          int res;
          do {
            res = channel.write(buffer);
            boolean flushed = channel.flush();
            while (!buffered.isEmpty()) {
              // figure out which messages are complete
              UndertowServerSentConnection.SSEData data = buffered.peek();
              if (data.endBufferPosition > 0 && buffer.position() >= data.endBufferPosition) {
                buffered.poll();
                if (flushed) {
                  if (data.callback != null && data.leftOverData == null) {
                    data.callback.done(UndertowServerSentConnection.this, data.message);
                  }
                } else {
                  // if flush was unsuccessful we defer the callback invocation, till it is actually
                  // on the wire
                  flushingMessages.add(data);
                }

              } else {
                if (data.endBufferPosition <= 0) {
                  buffered.poll();
                }
                break;
              }
            }
            if (!flushed && !flushingMessages.isEmpty()) {
              sink.resumeWrites();
              return;
            }

            if (!buffer.hasRemaining()) {
              fillBuffer();
              if (pooled == null) {
                return;
              }
            } else if (res == 0) {
              sink.resumeWrites();
              return;
            }

          } while (res > 0);
        } catch (IOException e) {
          handleException(e);
        }
      }
    }
  }

  private void handleException(IOException e) {
    IoUtils.safeClose(this, sink, exchange.getConnection());
  }
}
