/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.output.CompositeOutput;
import io.jooby.internal.output.OutputByteArrayStatic;
import io.jooby.internal.output.OutputStatic;
import io.jooby.internal.output.WrappedOutput;

/**
 * An output factory backed by {@link ByteBuffer}.
 *
 * @author edgar
 * @since 4.0.0
 */
public class ByteBufferedOutputFactory implements OutputFactory {

  private static class ContextOutputFactory extends ByteBufferedOutputFactory {
    public ContextOutputFactory(OutputOptions options) {
      super(options);
    }

    @Override
    public Output wrap(@NonNull ByteBuffer buffer) {
      return new WrappedOutput(buffer);
    }

    @Override
    public Output wrap(@NonNull String value, @NonNull Charset charset) {
      return new OutputByteArrayStatic(value.getBytes(charset));
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

  private final OutputOptions options;

  public ByteBufferedOutputFactory(OutputOptions options) {
    this.options = options;
  }

  @Override
  public OutputOptions getOptions() {
    return options;
  }

  @Override
  public BufferedOutput allocate(boolean direct, int size) {
    return new ByteBufferedOutput(direct, size);
  }

  @Override
  public BufferedOutput newComposite() {
    return new CompositeOutput();
  }

  @Override
  public Output wrap(@NonNull String value, @NonNull Charset charset) {
    return wrap(value.getBytes(charset));
  }

  @Override
  public Output wrap(@NonNull ByteBuffer buffer) {
    return new OutputStatic(buffer);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes) {
    return new OutputByteArrayStatic(bytes);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return new OutputByteArrayStatic(bytes, offset, length);
  }

  @Override
  public OutputFactory getContextFactory() {
    return new ContextOutputFactory(options);
  }
}
