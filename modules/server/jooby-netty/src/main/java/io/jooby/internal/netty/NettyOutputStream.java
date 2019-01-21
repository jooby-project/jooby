package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.IOException;
import java.io.OutputStream;

public class NettyOutputStream extends OutputStream {
  private final ByteBuf buffer;
  private final ChannelHandlerContext ctx;
  private final ChannelFutureListener closeListener;
  private HttpResponse headers;

  public NettyOutputStream(ChannelHandlerContext ctx, int bufferSize, HttpResponse headers,
      ChannelFutureListener closeListener) {
    this.buffer = ctx.alloc().buffer(0, bufferSize);
    this.ctx = ctx;
    this.headers = headers;
    this.closeListener = closeListener;
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
      ctx.write(headers, ctx.voidPromise());
      headers = null;
    }
  }

  @Override public void flush() throws IOException {
    flush(null, null);
  }

  private void flush(ChannelFutureListener callback, ChannelFutureListener listener) {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      if (listener != null) {
        if (callback == null) {
          ctx.write(new DefaultHttpContent(buffer.copy()), ctx.voidPromise());
        } else {
          ctx.write(new DefaultHttpContent(buffer.copy())).addListener(callback);
        }
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(listener);
        buffer.release();
      } else {
        if (callback == null) {
          ctx.write(new DefaultHttpContent(buffer.copy()), ctx.voidPromise());
        } else {
          ctx.write(new DefaultHttpContent(buffer.copy())).addListener(callback);
        }
        buffer.clear();
      }
    } else {
      ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(listener);
    }
  }

  @Override
  public void close() {
    flush(null, closeListener);
  }
}
