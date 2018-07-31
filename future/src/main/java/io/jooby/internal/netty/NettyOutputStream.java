package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;

import java.io.IOException;
import java.io.OutputStream;

public class NettyOutputStream extends OutputStream {
  private final ByteBuf buffer;

  private final ChannelHandlerContext ctx;

  public NettyOutputStream(ChannelHandlerContext ctx, int chunksize) {
    this.buffer = ctx.alloc().buffer(0, chunksize);
    this.ctx = ctx;
  }

  @Override
  public void write(int b) throws IOException {
    if (buffer.maxWritableBytes() < 1) {
      flush();
    }
    buffer.writeByte(b);
  }

  @Override
  public void close() throws IOException {
    try {
      flush();
    } finally {
      buffer.release();
    }
  }

  @Override
  public void write(byte[] bytes, int off, int len) throws IOException {
    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;
    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(bytes, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      flush();
    }
    if (dataLengthLeftToWrite > 0) {
      buffer.writeBytes(bytes, dataToWriteOffset, dataLengthLeftToWrite);
    }
  }

  @Override
  public void flush() throws IOException {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      ctx.writeAndFlush(new DefaultHttpContent(buffer.copy()));
      buffer.clear();
    }
  }
}
