/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class IOUtils {

  public static final String toString(InputStream in, Charset charset) throws IOException {
    try {
      return new String(in.readAllBytes(), charset);
    } finally {
      in.close();
    }
  }

  public static final InputStream bounded(InputStream in, long start, long size)
      throws IOException {
    in.skip(start);
    return new BoundedInputStream(in, size);
  }

  // Copy from org.apache.commons.io.input.BoundedInputStream
  private static class BoundedInputStream extends InputStream {

    private static final int EOF = -1;

    /** the wrapped input stream */
    private final InputStream in;

    /** the max length to provide */
    private final long max;

    /** the number of bytes already returned */
    private long pos;

    /** the marked position */
    private long mark = EOF;

    /** flag if close should be propagated */
    private boolean propagateClose = true;

    /**
     * Creates a new {@code BoundedInputStream} that wraps the given input stream and limits it to a
     * certain size.
     *
     * @param in The wrapped input stream
     * @param size The maximum number of bytes to return
     */
    public BoundedInputStream(final InputStream in, final long size) {
      // Some badly designed methods - eg the servlet API - overload length
      // such that "-1" means stream finished
      this.max = size;
      this.in = in;
    }

    /**
     * Creates a new {@code BoundedInputStream} that wraps the given input stream and is unlimited.
     *
     * @param in The wrapped input stream
     */
    public BoundedInputStream(final InputStream in) {
      this(in, EOF);
    }

    /**
     * Invokes the delegate's {@code read()} method if the current position is less than the limit.
     *
     * @return the byte read or -1 if the end of stream or the limit has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
      if (max >= 0 && pos >= max) {
        return EOF;
      }
      final int result = in.read();
      pos++;
      return result;
    }

    /**
     * Invokes the delegate's {@code read(byte[])} method.
     *
     * @param b the buffer to read the bytes into
     * @return the number of bytes read or -1 if the end of stream or the limit has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b) throws IOException {
      return this.read(b, 0, b.length);
    }

    /**
     * Invokes the delegate's {@code read(byte[], int, int)} method.
     *
     * @param b the buffer to read the bytes into
     * @param off The start offset
     * @param len The number of bytes to read
     * @return the number of bytes read or -1 if the end of stream or the limit has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      if (max >= 0 && pos >= max) {
        return EOF;
      }
      final long maxRead = max >= 0 ? Math.min(len, max - pos) : len;
      final int bytesRead = in.read(b, off, (int) maxRead);

      if (bytesRead == EOF) {
        return EOF;
      }

      pos += bytesRead;
      return bytesRead;
    }

    /**
     * Invokes the delegate's {@code skip(long)} method.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public long skip(final long n) throws IOException {
      final long toSkip = max >= 0 ? Math.min(n, max - pos) : n;
      final long skippedBytes = in.skip(toSkip);
      pos += skippedBytes;
      return skippedBytes;
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
      if (max >= 0 && pos >= max) {
        return 0;
      }
      return in.available();
    }

    /**
     * Invokes the delegate's {@code toString()} method.
     *
     * @return the delegate's {@code toString()}
     */
    @Override
    public String toString() {
      return in.toString();
    }

    /**
     * Invokes the delegate's {@code close()} method if {@link #isPropagateClose()} is {@code true}.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
      if (propagateClose) {
        in.close();
      }
    }

    /**
     * Invokes the delegate's {@code reset()} method.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void reset() throws IOException {
      in.reset();
      pos = mark;
    }

    /**
     * Invokes the delegate's {@code mark(int)} method.
     *
     * @param readlimit read ahead limit
     */
    @Override
    public synchronized void mark(final int readlimit) {
      in.mark(readlimit);
      mark = pos;
    }

    /**
     * Invokes the delegate's {@code markSupported()} method.
     *
     * @return true if mark is supported, otherwise false
     */
    @Override
    public boolean markSupported() {
      return in.markSupported();
    }

    /**
     * Indicates whether the {@link #close()} method should propagate to the underling {@link
     * InputStream}.
     *
     * @return {@code true} if calling {@link #close()} propagates to the {@code close()} method of
     *     the underlying stream or {@code false} if it does not.
     */
    public boolean isPropagateClose() {
      return propagateClose;
    }

    /**
     * Set whether the {@link #close()} method should propagate to the underling {@link
     * InputStream}.
     *
     * @param propagateClose {@code true} if calling {@link #close()} propagates to the {@code
     *     close()} method of the underlying stream or {@code false} if it does not.
     */
    public void setPropagateClose(final boolean propagateClose) {
      this.propagateClose = propagateClose;
    }
  }
}
