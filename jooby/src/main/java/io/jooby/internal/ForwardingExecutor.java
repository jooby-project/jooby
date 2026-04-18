/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.concurrent.Executor;

public class ForwardingExecutor implements Executor {
  Executor executor;

  @Override
  public void execute(Runnable command) {
    if (executor == null) {
      throw new IllegalStateException("Worker executor not ready");
    }
    executor.execute(command);
  }
}
