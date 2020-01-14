/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.io.IOException;
import java.io.OutputStream;

public class JettyOutputStream extends OutputStream {

  private JettyContext jetty;
  private OutputStream out;

  public JettyOutputStream(OutputStream out, JettyContext jetty) {
    this.out = out;
    this.jetty = jetty;
  }

  @Override public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
  }

  @Override public void write(byte[] b) throws IOException {
    out.write(b);
  }

  @Override public void write(int b) throws IOException {
    out.write(b);
  }

  @Override public void flush() throws IOException {
    out.flush();
  }

  @Override public void close() throws IOException {
    try {
      out.close();
    } finally {
      jetty.responseDone();
    }
  }
}
