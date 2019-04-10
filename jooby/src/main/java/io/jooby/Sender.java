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
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Non-blocking sender. Reactive responses uses this class to send partial data in non-blocking
 * manner.
 *
 * RxJava example:
 *
 * <pre>{@code
 *
 *   Sender sender = ctx.getSender();
 *
 *   Flux.fromCallable(...)
 *     .subscribe(new Subscriber () {
 *
 *       onSubscribe(Subscription s) {
 *         this.subscription = s;
 *         this.subscription.request(1);
 *       }
 *
 *       onNext(Object next) {
 *         sender.write(next, (ctx, cause) -> {
 *           subscription.request(1);
 *         });
 *       }
 *
 *       onError(Throwable error) {
 *         subscription.cancel();
 *       }
 *
 *       onComplete() {
 *         sender.close();
 *       }
 *     })
 *
 * }</pre>
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Sender {

  /**
   * Write callback.
   */
  interface Callback {
    /**
     * Callback after for <code>write</code> operation.
     *
     * @param ctx Web context.
     * @param cause Cause in case of error or <code>null</code> for success.
     */
    void onComplete(@Nonnull Context ctx, @Nullable Throwable cause);
  }

  /**
   * Write a string chunk. Chunk is flushed immediately.
   *
   * @param data String chunk.
   * @param callback Callback.
   * @return This sender.
   */
  @Nonnull default Sender write(@Nonnull String data, @Nonnull Callback callback) {
    return write(data, StandardCharsets.UTF_8, callback);
  }

  /**
   * Write a string chunk. Chunk is flushed immediately.
   *
   * @param data String chunk.
   * @param charset Charset.
   * @param callback Callback.
   * @return This sender.
   */
  @Nonnull default Sender write(@Nonnull String data, @Nonnull Charset charset,
      @Nonnull Callback callback) {
    return write(data.getBytes(charset), callback);
  }

  /**
   * Write a bytes chunk. Chunk is flushed immediately.
   *
   * @param data Bytes chunk.
   * @param callback Callback.
   * @return This sender.
   */
  @Nonnull Sender write(@Nonnull byte[] data, @Nonnull Callback callback);

  /**
   * Close the sender.
   */
  void close();
}
