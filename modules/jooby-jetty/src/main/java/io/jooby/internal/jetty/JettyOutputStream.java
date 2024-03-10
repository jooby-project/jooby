/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.io.OutputStream;

import io.jooby.SneakyThrows;

public class JettyOutputStream extends OutputStream {

  private JettyContext jetty;
  private OutputStream out;

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
    block(out::close, true);
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
