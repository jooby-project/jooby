/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.jooby.internal.GracefulShutdownHandler;

public class GracefulShutdownTest {

  @Test
  public void installWithDefaultConstructor() throws Exception {
    Jooby app = mock(Jooby.class);
    GracefulShutdown extension = new GracefulShutdown();

    extension.install(app);

    // Verify a handler was added to the route pipeline
    verify(app).use(any(GracefulShutdownHandler.class));
    // Verify a shutdown task was registered
    verify(app).onStop(any(AutoCloseable.class));
  }

  @Test
  public void installWithTimeout() throws Exception {
    Jooby app = mock(Jooby.class);
    Duration timeout = Duration.ofSeconds(5);
    GracefulShutdown extension = new GracefulShutdown(timeout);

    extension.install(app);

    // Verify the handler and stop callback are registered
    verify(app).use(any(GracefulShutdownHandler.class));
    verify(app).onStop(any(AutoCloseable.class));
  }
}
