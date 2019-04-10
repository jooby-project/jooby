/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
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

  @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
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
