/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.buffer;

import java.io.IOException;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.buffer.BufferedOutput;

/**
 * An {@link OutputStream} that writes to a {@link BufferedOutput}.
 *
 * @see BufferedOutput#asOutputStream()
 */
public class OutputOutputStream extends OutputStream {

  private final BufferedOutput output;

  private boolean closed;

  public OutputOutputStream(@NonNull BufferedOutput output) {
    this.output = output;
  }

  @Override
  public void write(int b) throws IOException {
    checkClosed();
    this.output.write((byte) b);
  }

  @Override
  public void write(@NonNull byte[] b, int off, int len) throws IOException {
    checkClosed();
    if (len > 0) {
      this.output.write(b, off, len);
    }
  }

  @Override
  public void close() throws IOException {
    if (this.closed) {
      return;
    }
    this.closed = true;
    output.clear();
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("OutputStream is closed");
    }
  }
}
