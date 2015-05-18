package org.jooby.internal.undertow;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.xnio.IoUtils;
import org.xnio.Pooled;

public class ChunkedStream implements IoCallback, Runnable {

  private ReadableByteChannel source;

  private HttpServerExchange exchange;

  private Sender sender;

  private Pooled<ByteBuffer> pooled;

  private IoCallback callback;

  private int bufferSize;

  private int chunk;

  public void send(final ReadableByteChannel source, final HttpServerExchange exchange,
      final IoCallback callback) {
    this.source = source;
    this.exchange = exchange;
    this.callback = callback;
    this.sender = exchange.getResponseSender();
    ServerConnection connection = exchange.getConnection();
    this.pooled = connection.getBufferPool().allocate();
    this.bufferSize = connection.getBufferSize();

    onComplete(exchange, sender);
  }

  @Override
  public void run() {
    ByteBuffer buffer = pooled.getResource();
    chunk += 1;
    try {
      buffer.clear();
      int count = source.read(buffer);
      if (count == -1) {
        done();
        callback.onComplete(exchange, sender);
      } else {
        if (chunk == 1) {
          if (count < bufferSize) {
            HeaderMap headers = exchange.getResponseHeaders();
            if (!headers.contains(Headers.CONTENT_LENGTH)) {
              headers.put(Headers.CONTENT_LENGTH, count);
              headers.remove(Headers.TRANSFER_ENCODING);
            }
          } else {
            HeaderMap headers = exchange.getResponseHeaders();
            // just check if
            if (!headers.contains(Headers.CONTENT_LENGTH)) {
              headers.put(Headers.TRANSFER_ENCODING, "chunked");
            }
          }
        }
        buffer.flip();
        sender.send(buffer, this);
      }
    } catch (IOException ex) {
      onException(exchange, sender, ex);
    }
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
    } else {
      run();
    }
  }

  @Override
  public void onException(final HttpServerExchange exchange, final Sender sender,
      final IOException ex) {
    done();
    callback.onException(exchange, sender, ex);
  }

  private void done() {
    pooled.free();
    pooled = null;
    IoUtils.safeClose(source);
  }

}
