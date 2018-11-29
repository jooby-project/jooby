package io.jooby;

public enum Mode {
  /**
   * Run in non-blocking IO if the route returns:
   * - A {@link java.util.concurrent.CompletableFuture}.
   * - A {@link java.util.concurrent.Flow.Publisher}.
   * - An Observable, Flowable, etc.
   */
  DEFAULT,

  /** Non-blocking I/O (a.k.a. event loop) */
  NIO,

  /** Blocking IO (a.k.a. worker thread) */
  IO
}
