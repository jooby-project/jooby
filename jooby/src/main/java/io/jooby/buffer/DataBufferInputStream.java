/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} that reads from a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see DataBuffer#asInputStream(boolean)
 */
final class DataBufferInputStream extends InputStream {

  private final DataBuffer dataBuffer;

  private final int end;

  private final boolean releaseOnClose;

  private boolean closed;

  private int mark;

  public DataBufferInputStream(DataBuffer dataBuffer, boolean releaseOnClose) {
    Assert.notNull(dataBuffer, "DataBuffer must not be null");
    this.dataBuffer = dataBuffer;
    int start = this.dataBuffer.readPosition();
    this.end = start + this.dataBuffer.readableByteCount();
    this.mark = start;
    this.releaseOnClose = releaseOnClose;
  }

  @Override
  public int read() throws IOException {
    checkClosed();
    if (available() == 0) {
      return -1;
    }
    return this.dataBuffer.read() & 0xFF;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    checkClosed();
    int available = available();
    if (available == 0) {
      return -1;
    }
    len = Math.min(available, len);
    this.dataBuffer.read(b, off, len);
    return len;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public void mark(int readLimit) {
    Assert.isTrue(readLimit > 0, "readLimit must be greater than 0");
    this.mark = this.dataBuffer.readPosition();
  }

  @Override
  public int available() {
    return Math.max(0, this.end - this.dataBuffer.readPosition());
  }

  @Override
  public void reset() {
    this.dataBuffer.readPosition(this.mark);
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    if (this.releaseOnClose) {
      DataBufferUtils.release(this.dataBuffer);
    }
    this.closed = true;
  }

  private void checkClosed() throws IOException {
    if (this.closed) {
      throw new IOException("DataBufferInputStream is closed");
    }
  }
}
