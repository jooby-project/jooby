/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Route;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.LongUnaryOperator;

public class GracefulShutdownHandler implements Route.Decorator {
  private static final long SHUTDOWN_MASK = 1L << 63;
  private static final long ACTIVE_COUNT_MASK = (1L << 63) - 1;

  private static final LongUnaryOperator incrementActive = current -> {
    long incrementedActiveCount = activeCount(current) + 1;
    return incrementedActiveCount | (current & ~ACTIVE_COUNT_MASK);
  };

  private static final LongUnaryOperator incrementActiveAndShutdown =
      incrementActive.andThen(current -> current | SHUTDOWN_MASK);

  private static final LongUnaryOperator decrementActive = current -> {
    long decrementedActiveCount = activeCount(current) - 1;
    return decrementedActiveCount | (current & ~ACTIVE_COUNT_MASK);
  };

  private final Object lock = new Object();

  private final Duration await;

  private volatile long state = 0;
  private static final AtomicLongFieldUpdater<GracefulShutdownHandler> stateUpdater =
      AtomicLongFieldUpdater.newUpdater(GracefulShutdownHandler.class, "state");

  public GracefulShutdownHandler(Duration await) {
    this.await = await;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      long snapshot = stateUpdater.updateAndGet(this, incrementActive);
      if (isShutdown(snapshot)) {
        decrementRequests();
        return ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE)
            .send(StatusCode.SERVICE_UNAVAILABLE);
      } else {
        ctx.onComplete(context -> {
          decrementRequests();
        });
        return next.apply(ctx);
      }
    };
  }

  private static boolean isShutdown(long state) {
    return (state & SHUTDOWN_MASK) != 0;
  }

  private static long activeCount(long state) {
    return state & ACTIVE_COUNT_MASK;
  }

  public void shutdown() throws InterruptedException {
    //the request count is never zero when shutdown is set to true
    stateUpdater.updateAndGet(this, incrementActiveAndShutdown);
    decrementRequests();

    if (await == null) {
      awaitShutdown();
    } else {
      awaitShutdown(await.toMillis());
    }
  }

  public void start() {
    synchronized (lock) {
      stateUpdater.updateAndGet(this, current -> current & ACTIVE_COUNT_MASK);
    }
  }

  private void shutdownComplete() {
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  /**
   * Waits for the handler to shutdown.
   */
  private void awaitShutdown() throws InterruptedException {
    synchronized (lock) {
      if (!isShutdown(stateUpdater.get(this))) {
        return;
      }
      while (activeCount(stateUpdater.get(this)) > 0) {
        lock.wait();
      }
    }
  }

  /**
   * Waits a set length of time for the handler to shut down
   *
   * @param millis The length of time
   * @return <code>true</code> If the handler successfully shut down
   */
  private boolean awaitShutdown(long millis) throws InterruptedException {
    synchronized (lock) {
      if (!isShutdown(stateUpdater.get(this))) {
        return false;
      }
      long end = System.currentTimeMillis() + millis;
      while (activeCount(stateUpdater.get(this)) != 0) {
        long left = end - System.currentTimeMillis();
        if (left <= 0) {
          return false;
        }
        lock.wait(left);
      }
      return true;
    }
  }

  private void decrementRequests() {
    long snapshot = stateUpdater.updateAndGet(this, decrementActive);
    // Shutdown has completed when the activeCount portion is zero, and shutdown is set.
    if (snapshot == SHUTDOWN_MASK) {
      shutdownComplete();
    }
  }
}
