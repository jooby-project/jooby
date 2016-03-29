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
package org.jooby.internal.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;

import org.jooby.Sse;

import com.google.common.util.concurrent.MoreExecutors;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import javaslang.concurrent.Promise;

public class UndertowSse extends Sse {

  private static class DoneCallback implements IoCallback {

    private Promise<Optional<Object>> promise;

    private Consumer<Throwable> ifClose;

    private Optional<Object> id;

    public DoneCallback(final Promise<Optional<Object>> promise, final Optional<Object> id,
        final Consumer<Throwable> ifClose) {
      this.promise = promise;
      this.id = id;
      this.ifClose = ifClose;
    }

    @Override
    public void onComplete(final HttpServerExchange exchange, final Sender sender) {
      promise.success(id);
    }

    @Override
    public void onException(final HttpServerExchange exchange, final Sender sender,
        final IOException cause) {
      promise.failure(cause);
      ifClose.accept(cause);
    }
  }

  private HttpServerExchange exchange;

  public UndertowSse(final HttpServerExchange exchange) throws Exception {
    this.exchange = exchange;
  }

  @Override
  protected void closeInternal() {
    exchange.endExchange();
  }

  @Override
  protected void handshake(final Runnable handler) throws Exception {
    exchange.getResponseHeaders()
        .put(Headers.CONNECTION, "Close")
        .put(Headers.CONTENT_TYPE, "text/event-stream; charset=utf-8");
    exchange.setStatusCode(200)
        .setPersistent(false);

    exchange.dispatch(handler);
  }

  @Override
  protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
    synchronized (this) {
      Promise<Optional<Object>> promise = Promise.make(MoreExecutors.newDirectExecutorService());
      Sender sender = exchange.getResponseSender();
      sender.send(ByteBuffer.wrap(data), new DoneCallback(promise, id, this::ifClose));
      return promise;
    }
  }

}
