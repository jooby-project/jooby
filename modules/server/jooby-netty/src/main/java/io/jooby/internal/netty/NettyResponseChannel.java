/**
 *                                  Apache License
 *                            Version 2.0, January 2004
 *                         http://www.apache.org/licenses/LICENSE-2.0
 *                         Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class NettyResponseChannel implements WritableByteChannel, Closeable {

  private ChannelHandlerContext ctx;
  private HttpResponse rsp;
  private final int maxBufferSize;
  private int bufferSize;

  public NettyResponseChannel(ChannelHandlerContext ctx, HttpResponse rsp, int maxBufferSize) {
    this.ctx = ctx;
    this.rsp = rsp;
    this.maxBufferSize = maxBufferSize;
  }

  @Override public int write(ByteBuffer src) {
    int size = src.remaining();
    boolean flush = size + bufferSize >= maxBufferSize;

    if (rsp != null) {
      ctx.write(rsp, ctx.voidPromise());
      rsp = null;
    }

    ByteBuf bytebuf = Unpooled.wrappedBuffer(src).copy();
    if (flush) {
      bufferSize = 0;
      ctx.writeAndFlush(new DefaultHttpContent(bytebuf), ctx.voidPromise());
    } else {
      bufferSize += size;
      ctx.write(new DefaultHttpContent(bytebuf), ctx.voidPromise());
    }

    return size;
  }

  @Override public boolean isOpen() {
    return ctx != null;
  }

  @Override public void close() {
    if (ctx != null) {
      ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, ctx.voidPromise());
      ctx = null;
    }
  }
}
