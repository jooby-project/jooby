/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyOutputStream extends OutputStream {
  private final ByteBuf buffer;
  private final NettyContext ctx;
  private final ChannelFutureListener closeListener;
  private HttpResponse headers;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public NettyOutputStream(
      NettyContext ctx, ChannelHandlerContext context, int bufferSize, HttpResponse headers) {
    this.ctx = ctx;
    this.buffer = context.alloc().heapBuffer(bufferSize, bufferSize);
    this.headers = headers;
    this.closeListener = ctx;
  }

  @Override
  public void write(int b) {
    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public void write(byte[] src, int off, int len) {
    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;

    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(src, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      flush(false);
    }
    if (dataLengthLeftToWrite > 0) {
      buffer.writeBytes(src, dataToWriteOffset, dataLengthLeftToWrite);
    }
  }

  @Override
  public void flush() throws IOException {
    flush(false);
  }

  private void flush(boolean close) {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      ByteBuf chunk;
      if (close) {
        // don't copy on close
        chunk = buffer;
      } else {
        // make a copy on flush
        chunk = buffer.copy();
      }
      ctx.connection.write(
          context -> {
            writeHeaders(context);
            context.write(new DefaultHttpContent(chunk), context.voidPromise());
            if (close) {
              context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(closeListener);
            }
          });
      if (!close) {
        // reset
        buffer.clear();
      }
    } else {
      ctx.connection.write(
          context -> {
            writeHeaders(context);
            ChannelFuture future = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (close) {
              future.addListener(closeListener);
            }
          });
    }
  }

  private void writeHeaders(ChannelHandlerContext context) {
    if (headers != null) {
      context.write(headers, context.voidPromise());
      headers = null;
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        flush(true);
      } finally {
        ctx.requestComplete();
      }
    }
  }
}
