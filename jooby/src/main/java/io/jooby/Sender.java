/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.output.Output;

/**
 * Non-blocking sender. Reactive responses use this class to send partial data in a non-blocking
 * manner.
 *
 * <p>RxJava example:
 *
 * <pre>{@code
 * Sender sender = ctx.getSender();
 *
 * Flux.fromCallable(...)
 *   .subscribe(new Subscriber () {
 *
 *     onSubscribe(Subscription s) {
 *       this.subscription = s;
 *       this.subscription.request(1);
 *     }
 *
 *     onNext(Object next) {
 *       sender.write(next, (ctx, cause) -> {
 *         subscription.request(1);
 *       });
 *     }
 *
 *     onError(Throwable error) {
 *       subscription.cancel();
 *     }
 *
 *     onComplete() {
 *       sender.close();
 *     }
 *   })
 *
 * }</pre>
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Sender {

  /** Write callback. */
  interface Callback {
    /**
     * Callback after for <code>write</code> operation.
     *
     * @param ctx Web context.
     * @param cause Cause in case of error or <code>null</code> for success.
     */
    void onComplete(@NonNull Context ctx, @Nullable Throwable cause);
  }

  /**
   * Write a string chunk. Chunk is flushed immediately.
   *
   * @param data String chunk.
   * @param callback Callback.
   * @return This sender.
   */
  @NonNull default Sender write(@NonNull String data, @NonNull Callback callback) {
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
  @NonNull default Sender write(@NonNull String data, @NonNull Charset charset, @NonNull Callback callback) {
    return write(data.getBytes(charset), callback);
  }

  /**
   * Write a byte chunk. Chunk is flushed immediately.
   *
   * @param data Bytes chunk.
   * @param callback Callback.
   * @return This sender.
   */
  Sender write(@NonNull byte[] data, @NonNull Callback callback);

  Sender write(@NonNull Output output, @NonNull Callback callback);

  /** Close the sender. */
  void close();
}
