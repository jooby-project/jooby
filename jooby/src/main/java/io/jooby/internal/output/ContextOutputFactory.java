/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.output.ByteBufferedOutputFactory;
import io.jooby.output.Output;
import io.jooby.output.OutputOptions;

public class ContextOutputFactory extends ByteBufferedOutputFactory {
  public ContextOutputFactory(OutputOptions options) {
    super(options);
  }

  @Override
  public Output wrap(@NonNull ByteBuffer buffer) {
    return new WrappedOutput(buffer);
  }

  @Override
  public Output wrap(@NonNull String value, @NonNull Charset charset) {
    return new WrappedOutput(charset.encode(value));
  }

  @Override
  public Output wrap(@NonNull byte[] bytes) {
    return new WrappedOutput(ByteBuffer.wrap(bytes));
  }

  @Override
  public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return new WrappedOutput(ByteBuffer.wrap(bytes, offset, length));
  }
}
