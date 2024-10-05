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
  private final ChannelHandlerContext context;
  private final ChannelFutureListener closeListener;
  private HttpResponse headers;
  private AtomicBoolean closed = new AtomicBoolean(false);

  public NettyOutputStream(
      NettyContext ctx, ChannelHandlerContext context, int bufferSize, HttpResponse headers) {
    this.ctx = ctx;
    this.buffer = context.alloc().buffer(0, bufferSize);
    this.context = context;
    this.headers = headers;
    this.closeListener = ctx;
  }

  @Override
  public void write(int b) {
    writeHeaders();

    if (buffer.maxWritableBytes() < 1) {
      flush(null, null);
    }
    buffer.writeByte(b);
  }

  @Override
  public void write(byte[] src, int off, int len) {
    write(src, off, len, null);
  }

  public void write(byte[] src, int off, int len, ChannelFutureListener callback) {
    writeHeaders();

    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;

    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(src, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      flush(callback, null);
    }
    if (dataLengthLeftToWrite > 0) {
      buffer.writeBytes(src, dataToWriteOffset, dataLengthLeftToWrite);
    }
  }

  private void writeHeaders() {
    if (headers != null) {
      context.write(headers, context.voidPromise());
      headers = null;
    }
  }

  @Override
  public void flush() throws IOException {
    flush(null, null);
  }

  private void flush(ChannelFutureListener callback, ChannelFutureListener listener) {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      if (listener != null) {
        if (callback == null) {
          context.write(new DefaultHttpContent(buffer.copy()), context.voidPromise());
        } else {
          context.write(new DefaultHttpContent(buffer.copy())).addListener(callback);
        }
        context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(listener);
        buffer.release();
      } else {
        if (callback == null) {
          context.write(new DefaultHttpContent(buffer.copy()), context.voidPromise());
        } else {
          context.write(new DefaultHttpContent(buffer.copy())).addListener(callback);
        }
        buffer.clear();
      }
    } else {
      ChannelFuture future = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      if (listener != null) {
        future.addListener(listener);
      }
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        flush(null, closeListener);
      } finally {
        ctx.requestComplete();
      }
    }
  }
}
