/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.buffer.BufferedOutput;

public class OutputWriter extends Writer {
  private final BufferedOutput output;
  private final Charset charset;
  private boolean closed;

  public OutputWriter(@NonNull BufferedOutput output, @NonNull Charset charset) {
    this.output = output;
    this.charset = charset;
  }

  @Override
  public void write(int c) throws IOException {
    checkClosed();
    super.write(c);
  }

  @Override
  public void write(@NonNull char[] source) throws IOException {
    write(source, 0, source.length);
  }

  @Override
  public void write(@NonNull char[] source, int off, int len) throws IOException {
    checkClosed();
    output.write(CharBuffer.wrap(source, off, len), charset);
  }

  @Override
  public void write(@NonNull String source) throws IOException {
    checkClosed();
    output.write(source, charset);
  }

  @Override
  public void write(@NonNull String source, int off, int len) throws IOException {
    checkClosed();
    output.write(CharBuffer.wrap(source, off, off + len), charset);
  }

  @Override
  public void flush() throws IOException {}

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
      throw new IOException("Writer is closed");
    }
  }
}
