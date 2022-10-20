/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.time.Duration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.GracefulShutdownHandler;

/**
 * Install a handler that at application shutdown time:
 *
 * <p>- Waits for existing requests to finished with an optional timeout - Incoming requests are
 * resolved as Service Unavailable(503)
 *
 * <p>NOTE: This extension must be installed at very beginning of your route pipeline.
 *
 * @author edgar
 */
public class GracefulShutdown implements Extension {
  private Duration await;

  /**
   * Creates a new shutdown handler and waits for existing requests to finish or for specified
   * amount of time.
   *
   * @param await Max time to wait for handlers to complete.
   */
  public GracefulShutdown(@NonNull Duration await) {
    this.await = await;
  }

  /** Creates a new shutdown handler and waits for existing request to finish. */
  public GracefulShutdown() {}

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    GracefulShutdownHandler handler = new GracefulShutdownHandler(await);
    application.decorator(handler);
    application.onStop(handler::shutdown);
  }
}
