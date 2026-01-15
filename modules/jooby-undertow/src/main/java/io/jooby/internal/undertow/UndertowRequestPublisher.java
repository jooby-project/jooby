/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static io.undertow.io.IoCallback.END_EXCHANGE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSourceChannel;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.io.Receiver;
import io.undertow.server.Connectors;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class UndertowRequestPublisher implements Flow.Publisher<byte[]> {

  public static final AttachmentKey<StreamSourceChannel> REQUEST_CHANNEL =
      AttachmentKey.create(StreamSourceChannel.class);

  private final HttpServerExchange exchange;

  public UndertowRequestPublisher(HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
    // We use the Subscription to manage the state between Undertow and the Subscriber
    UndertowReceiverSubscription sub = new UndertowReceiverSubscription(exchange, subscriber);
    subscriber.onSubscribe(sub);
  }
}

class UndertowReceiverSubscription implements Flow.Subscription {
  private static final Logger log = LoggerFactory.getLogger(UndertowReceiverSubscription.class);
  private final HttpServerExchange exchange;
  private final Flow.Subscriber<? super byte[]> subscriber;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicLong demand = new AtomicLong(0);
  private final AtomicBoolean readingStarted = new AtomicBoolean(false);
  private UndertowReceiver receiver;

  public UndertowReceiverSubscription(
      HttpServerExchange exchange, Flow.Subscriber<? super byte[]> subscriber) {
    this.exchange = exchange;
    this.subscriber = subscriber;
  }

  @Override
  public void request(long n) {
    if (n <= 0) return;
    log.info("init request({})", n);
    // if (receiver == null) {
    // receiver = new UndertowReceiver(exchange, () -> {});
    process();
    //    } else {
    //      receiver.resume();
    //    }
  }

  private void process() {
    var call = new AtomicInteger(0);

    //    var receiver = exchange.getRequestReceiver();
    exchange
        .getRequestReceiver()
        .receivePartialBytes(
            (exch, message, last) -> {
              call.incrementAndGet();
              log.info("{}- byte len: {}", call, message.length);
              if (message.length > 0) {
                // Pass bytes to De-framer
                log.info("{}- byte read: {}", call, HexFormat.of().formatHex(message));
                subscriber.onNext(message);
              }
              // THE KEY FIX:
              // 1. If 'last' is true, the stream is definitely over.
              // 2. If 'isRequestComplete' is true, Undertow's internal state knows it's over.
              if (last) {
                log.info("{}- last reach", call);
                subscriber.onComplete();
              }
            },
            (exch, err) -> {
              subscriber.onError(err);
            });
  }

  @Override
  public void cancel() {
    exchange.getRequestReceiver().pause();
  }
}

class UndertowReceiver {
  private final Logger log = LoggerFactory.getLogger(UndertowReceiver.class);
  private final HttpServerExchange exchange;
  private final StreamSourceChannel channel;
  private final Runnable runnable;
  private int maxBufferSize = -1;
  private boolean paused = false;
  private boolean done = false;
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  private static final Receiver.ErrorCallback END_EXCHANGE =
      new Receiver.ErrorCallback() {
        @Override
        public void error(HttpServerExchange exchange, IOException e) {
          e.printStackTrace();
          exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
          UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
          exchange.endExchange();
        }
      };

  public UndertowReceiver(HttpServerExchange exchange, Runnable runnable) {
    this.exchange = exchange;
    this.channel = exchange.getRequestChannel();
    exchange.putAttachment(UndertowRequestPublisher.REQUEST_CHANNEL, this.channel);
    this.runnable = runnable;
  }

  public void receivePartialBytes(
      final Receiver.PartialBytesCallback callback, final Receiver.ErrorCallback errorCallback) {
    if (done) {
      throw UndertowMessages.MESSAGES.requestBodyAlreadyRead();
    }
    final Receiver.ErrorCallback error = errorCallback == null ? END_EXCHANGE : errorCallback;
    if (callback == null) {
      throw UndertowMessages.MESSAGES.argumentCannotBeNull("callback");
    }
    if (exchange.isRequestComplete()) {
      log.info("request complete");
      callback.handle(exchange, EMPTY_BYTE_ARRAY, true);
      return;
    }
    String contentLengthString = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
    if (contentLengthString == null) {
      contentLengthString = exchange.getRequestHeaders().getFirst(Headers.X_CONTENT_LENGTH);
    }
    long contentLength;
    if (contentLengthString != null) {
      contentLength = Long.parseLong(contentLengthString);
      if (contentLength > Integer.MAX_VALUE) {
        error.error(exchange, new Receiver.RequestToLargeException());
        return;
      }
    } else {
      contentLength = -1;
    }
    if (maxBufferSize > 0) {
      if (contentLength > maxBufferSize) {
        error.error(exchange, new Receiver.RequestToLargeException());
        return;
      }
    }
    PooledByteBuffer pooled = exchange.getConnection().getByteBufferPool().allocate();
    final ByteBuffer buffer = pooled.getBuffer();

    channel
        .getReadSetter()
        .set(
            channel -> {
              if (done || paused) {
                log.info("request done: {} or paused: {}", done, paused);
                return;
              }
              PooledByteBuffer pooled1 = exchange.getConnection().getByteBufferPool().allocate();
              final ByteBuffer buffer1 = pooled1.getBuffer();
              try {
                int res2;
                do {
                  if (paused) {
                    return;
                  }
                  try {
                    buffer1.clear();
                    res2 = channel.read(buffer1);
                    if (res2 == -1) {
                      done = true;
                      log.info("INSIDE request read done: {} ", res2);
                      Connectors.executeRootHandler(
                          exchange -> callback.handle(exchange, EMPTY_BYTE_ARRAY, true), exchange);
                      return;
                    } else if (res2 == 0) {
                      log.info("INSIDE  resume reads: {}", res2);
                      //              channel.resumeReads();
                      return;
                    } else {
                      buffer1.flip();
                      final byte[] data = new byte[buffer1.remaining()];
                      buffer1.get(data);

                      Connectors.executeRootHandler(
                          exchange -> {
                            callback.handle(exchange, data, false);
                            channel.resumeReads();
                          },
                          exchange);
                    }
                  } catch (final IOException e) {
                    log.info("INSIDE error reading from {}", exchange, e);
                    Connectors.executeRootHandler(exchange -> error.error(exchange, e), exchange);
                    return;
                  }
                } while (true);
              } finally {
                pooled1.close();
              }
            });

    try {
      int res;
      do {
        try {
          buffer.clear();
          res = channel.read(buffer);
          if (res == -1) {
            log.info("request read out-of listener: {} ", res);
            done = true;
            callback.handle(exchange, EMPTY_BYTE_ARRAY, true);
            return;
          } else if (res == 0) {
            log.info("request resume reads out-of listener: {} ", res);
            channel.resumeReads();
            return;
          } else {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            log.info("request read done out-of listener: {} ", res);
            callback.handle(exchange, data, false);
            if (paused) {
              return;
            }
          }
        } catch (IOException e) {
          error.error(exchange, e);
          return;
        }
      } while (true);
    } finally {
      log.info("channel open: {} ", channel.isOpen());
      pooled.close();
    }
  }

  public void resume() {
    channel.wakeupReads();
  }
}
