/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jooby.SneakyThrows;

public class JettyOutputStream extends OutputStream {

  private final JettyContext jetty;
  private final OutputStream out;
  private AtomicBoolean closed = new AtomicBoolean(false);

  public JettyOutputStream(OutputStream out, JettyContext jetty) {
    this.out = out;
    this.jetty = jetty;
  }

  @Override
  public void write(byte[] b, int off, int len) {
    block(() -> out.write(b, off, len), false);
  }

  @Override
  public void write(byte[] b) {
    block(() -> out.write(b), false);
  }

  @Override
  public void write(int b) {
    block(() -> out.write(b), false);
  }

  @Override
  public void flush() {
    block(out::flush, false);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      block(out::close, true);
    }
  }

  private void block(SneakyThrows.Runnable task, boolean complete) {
    try {
      task.run();
      if (complete) {
        jetty.succeeded();
      }
    } catch (Throwable cause) {
      jetty.failed(cause);
    }
  }
}
