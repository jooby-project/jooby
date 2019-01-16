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
  private final ChannelFutureListener listener;
  private HttpResponse headers;

  public NettyOutputStream(ChannelHandlerContext ctx, int bufferSize, HttpResponse headers,
      ChannelFutureListener listener) {
    this.buffer = ctx.alloc().buffer(0, bufferSize);
    this.ctx = ctx;
    this.headers = headers;
    this.listener = listener;
  }

  @Override
  public void write(int b) {
    writeHeaders();

    if (buffer.maxWritableBytes() < 1) {
      flush(null);
    }
    buffer.writeByte(b);
  }

  @Override
  public void write(byte[] src, int off, int len) {
    writeHeaders();

    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;

    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(src, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      flush(null);
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
    flush(null);
  }

  private void flush(ChannelFutureListener listener) {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      if (listener != null) {
        ctx.write(new DefaultHttpContent(buffer.copy()), ctx.voidPromise());
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(listener);
        buffer.release();
      } else {
        ctx.writeAndFlush(new DefaultHttpContent(buffer.copy()));
        buffer.clear();
      }
    } else {
      ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(listener);
    }
  }

  @Override
  public void close() {
    flush(listener);
  }
}
