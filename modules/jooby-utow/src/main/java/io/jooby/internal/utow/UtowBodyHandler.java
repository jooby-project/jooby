/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.Body;
import io.jooby.StatusCodeException;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.undertow.io.Receiver;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class UtowBodyHandler
    implements Receiver.FullBytesCallback, Receiver.PartialBytesCallback,
    ExchangeCompletionListener {

  private final int bufferSize;
  private final long maxRequestSize;
  private Router.Match route;
  private UtowContext context;
  private long chunkSize;
  private List chunks;
  private Path file;
  private FileChannel channel;
  private long position;

  public UtowBodyHandler(Router.Match route, UtowContext context, int bufferSize,
      long maxRequestSize) {
    this.route = route;
    this.context = context;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
  }

  @Override public void handle(HttpServerExchange exchange, byte[] bytes) {
    context.body = Body.of(context, bytes);
    route.execute(context);
  }

  @Override public void exchangeEvent(HttpServerExchange exchange, NextListener next) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException x) {
      // ignore
    } finally {
      next.proceed();
    }
  }

  @Override public void handle(HttpServerExchange exchange, byte[] chunk, boolean last) {
    try {
      if (chunk.length > 0) {
        chunkSize += chunk.length;
        if (chunkSize > maxRequestSize) {
          try {
            context.sendError(new StatusCodeException(StatusCode.REQUEST_ENTITY_TOO_LARGE));
          } finally {
            closeChannel();
            channel = null;
          }
          return;
        }
        if (chunkSize <= bufferSize) {
          if (chunks == null) {
            chunks = new ArrayList<>();
          }
          chunks.add(chunk);
        } else {
          // overflow
          if (file == null) {
            file = context.getRouter().getTmpdir().resolve("undertow" + System.nanoTime() + "body");
            channel = FileChannel.open(file, CREATE, WRITE);
          }
          if (chunks != null) {
            List source = chunks;
            chunks = null;
            for (Object s : source) {
              byte[] bytes = (byte[]) s;
              channel.write(ByteBuffer.wrap(bytes), position);
              position += bytes.length;
            }
            source.clear();
          }
          channel.write(ByteBuffer.wrap(chunk), position);
          position += chunk.length;
        }
      }
      if (last) {
        if (channel != null) {
          exchange.addExchangeCompleteListener(this);
          forceAndClose();
          channel = null;
          context.body = Body.of(context, file);
        } else {
          context.body = Body.of(context, bytes((int) chunkSize));
        }
        route.execute(context);
      }
    } catch (IOException x) {
      try {
        context.sendError(x);
      } finally {
        closeChannel();
        channel = null;
        exchange.endExchange();
      }
    }
  }

  private void closeChannel() {
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException x) {
        // close didn't succeed
      }
    }
  }

  private void forceAndClose() throws IOException {
    if (channel != null) {
      try {
        channel.force(true);
      } finally {
        closeChannel();
      }
    }
  }

  private byte[] bytes(int size) {
    byte[] bytes = new byte[size];
    int offset = 0;
    for (Object c : chunks) {
      byte[] chunk = (byte[]) c;
      System.arraycopy(chunk, 0, bytes, offset, chunk.length);
      offset += chunk.length;
    }
    return bytes;
  }
}
