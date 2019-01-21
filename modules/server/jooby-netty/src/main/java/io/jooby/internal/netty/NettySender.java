package io.jooby.internal.netty;

import io.jooby.Sender;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import javax.annotation.Nonnull;

public class NettySender implements Sender {

  private final NettyContext ctx;
  private final ChannelHandlerContext context;

  public NettySender(NettyContext ctx, ChannelHandlerContext context) {
    this.ctx = ctx;
    this.context = context;
  }

  @Override public Sender sendBytes(@Nonnull byte[] data, @Nonnull Callback callback) {
    context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data)))
        .addListener(newChannelFutureListener(ctx, callback));
    return this;
  }

  @Override public void close() {
    context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ctx);
  }

  private static ChannelFutureListener newChannelFutureListener(NettyContext ctx,
      Callback callback) {
    return future -> {
      if (future.isSuccess()) {
        callback.onComplete(ctx, null);
      } else {
        Throwable cause = future.cause();
        try {
          callback.onComplete(ctx, cause);
        } finally {
          ctx.destroy(cause);
        }
      }
    };
  }
}
