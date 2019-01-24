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
package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Sender;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public class UtowSender implements Sender {
  private final UtowContext ctx;
  private final HttpServerExchange exchange;

  public UtowSender(UtowContext ctx, HttpServerExchange exchange) {
    this.ctx = ctx;
    this.exchange = exchange;
  }

  @Override public Sender sendBytes(@Nonnull byte[] data, @Nonnull Callback callback) {
    exchange.getResponseSender().send(ByteBuffer.wrap(data), newIoCallback(ctx, callback));
    return this;
  }

  @Override public void close() {
    ctx.destroy(null);
  }

  private static IoCallback newIoCallback(UtowContext ctx, Callback callback) {
    return new IoCallback() {
      @Override public void onComplete(HttpServerExchange exchange, io.undertow.io.Sender sender) {
        callback.onComplete(ctx, null);
      }

      @Override public void onException(HttpServerExchange exchange, io.undertow.io.Sender sender,
          IOException exception) {
        try {
          callback.onComplete(ctx, exception);
        } finally {
          ctx.destroy(exception);
        }
      }
    };
  }
}
