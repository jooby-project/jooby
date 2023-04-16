/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jstach.jstachio.JStachio;

/*
 * Should this be public?
 */
class JStachioMessageEncoder implements MessageEncoder {

  private final JStachio jstachio;
  private final Charset charset;
  private final int bufferSize;

  public JStachioMessageEncoder(
      @NonNull JStachio jstachio, @NonNull Charset charset, int bufferSize) {
    super();
    this.jstachio = jstachio;
    this.charset = charset;
    this.bufferSize = bufferSize;
  }

  @Override
  public byte[] encode(Context ctx, Object value) throws Exception {
    if (supportsType(value.getClass())) {
      return render(value);
    }
    return null;
  }

  protected boolean supportsType(Class<?> modelClass) {
    return jstachio.supportsType(modelClass);
  }

  protected byte[] render(Object value) {
    StringBuilder b = acquireBuffer();
    try {
      jstachio.execute(value, b);
      return b.toString().getBytes(charset);
    } finally {
      releaseBuffer(b);
    }
  }

  /**
   * Returns a new buffer. {@link #releaseBuffer(StringBuilder)) should be
   * called when done.
   *
   * @return a buffer either from a pool or a new one.
   */
  protected StringBuilder acquireBuffer() {
    return new StringBuilder(bufferSize);
  }

  /**
   * Releases the buffer. Override for pooling buffers.
   *
   * @param sb
   */
  protected void releaseBuffer(StringBuilder sb) {
    return;
  }
}
