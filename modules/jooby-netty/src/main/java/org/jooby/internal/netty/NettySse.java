/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import java.util.Optional;
import java.util.function.Consumer;

import org.jooby.Sse;

import com.google.common.util.concurrent.MoreExecutors;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import javaslang.concurrent.Promise;

public class NettySse extends Sse {

  private static class DoneCallback implements ChannelFutureListener {

    private Promise<Optional<Object>> promise;

    private Consumer<Throwable> ifClose;

    private Optional<Object> id;

    public DoneCallback(final Promise<Optional<Object>> promise, final Optional<Object> id,
        final Consumer<Throwable> ifClose) {
      this.id = id;
      this.promise = promise;
      this.ifClose = ifClose;
    }

    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
      if (future.isSuccess()) {
        promise.success(id);
      } else {
        Throwable cause = future.cause();
        promise.failure(cause);
        ifClose.accept(cause);
      }
    }
  }

  private ChannelHandlerContext ctx;

  public NettySse(final ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected void closeInternal() {
    ctx.close();
  }

  @Override
  protected void handshake(final Runnable handler) throws Exception {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.set(HttpHeaderNames.CONNECTION, "Close");
    headers.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream; charset=utf-8");
    ctx.writeAndFlush(
        new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headers));
    ctx.executor().execute(handler);
  }

  @Override
  protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
    synchronized (this) {
      Promise<Optional<Object>> promise = Promise.make(MoreExecutors.newDirectExecutorService());
      ctx.writeAndFlush(Unpooled.wrappedBuffer(data))
          .addListener(new DoneCallback(promise, id, this::ifClose));
      return promise;
    }
  }

}
