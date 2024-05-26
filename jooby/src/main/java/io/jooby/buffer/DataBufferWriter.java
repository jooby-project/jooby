/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

class DataBufferWriter extends Writer {
  private final DataBuffer dataBuffer;
  private final Charset charset;

  private boolean closed;

  public DataBufferWriter(DataBuffer dataBuffer, Charset charset) {
    this.dataBuffer = dataBuffer;
    this.charset = charset;
  }

  @Override
  public void write(char[] buffer, int off, int len) throws IOException {
    checkClosed();
    dataBuffer.write(CharBuffer.wrap(buffer, off, len), charset);
  }

  @Override
  public void write(int c) throws IOException {
    checkClosed();
    super.write(c);
  }

  @Override
  public void write(String str) throws IOException {
    checkClosed();
    dataBuffer.write(str, charset);
  }

  @Override
  public void write(char[] cbuf) throws IOException {
    checkClosed();
    dataBuffer.write(CharBuffer.wrap(cbuf), charset);
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
    checkClosed();
    dataBuffer.write(CharBuffer.wrap(str, off, off + len), charset);
  }

  @Override
  public void flush() throws IOException {}

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferOutputStream is closed");
    }
  }
}
