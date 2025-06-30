/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Delegate/forwarding class for output factory.
 *
 * @author edgar
 * @since 4.0.0
 */
public abstract class ForwardingBufferedOutputFactory implements BufferedOutputFactory {

  protected final BufferedOutputFactory delegate;

  public ForwardingBufferedOutputFactory(@NonNull BufferedOutputFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public BufferOptions getOptions() {
    return delegate.getOptions();
  }

  @Override
  public BufferedOutputFactory setOptions(BufferOptions options) {
    delegate.setOptions(options);
    return this;
  }

  @Override
  public BufferedOutput newBufferedOutput(int size) {
    return delegate.newBufferedOutput(size);
  }

  @Override
  public BufferedOutput newBufferedOutput(boolean direct, int size) {
    return delegate.newBufferedOutput(direct, size);
  }

  @Override
  public BufferedOutput newCompositeOutput() {
    return delegate.newCompositeOutput();
  }

  @Override
  public BufferedOutput wrap(@NonNull ByteBuffer buffer) {
    return delegate.wrap(buffer);
  }

  @Override
  public BufferedOutput wrap(@NonNull byte[] bytes) {
    return delegate.wrap(bytes);
  }

  @Override
  public BufferedOutput wrap(@NonNull byte[] bytes, int offset, int length) {
    return delegate.wrap(bytes, offset, length);
  }
}
