package io.jooby.internal.handler.reactive;

public interface ChunkedSubscription {
  void request(long n);

  void cancel();
}
