/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Delegate/forwarding class for output factory.
 *
 * @author edgar
 * @since 4.0.0
 */
public abstract class ForwardingOutputFactory implements OutputFactory {

  protected final OutputFactory delegate;

  public ForwardingOutputFactory(@NonNull OutputFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getInitialBufferSize() {
    return delegate.getInitialBufferSize();
  }

  @Override
  public OutputFactory setInitialBufferSize(int initialBufferSize) {
    delegate.setInitialBufferSize(initialBufferSize);
    return this;
  }

  @Override
  public boolean isDirect() {
    return delegate.isDirect();
  }

  @Override
  public Output newBufferedOutput(int size) {
    return delegate.newBufferedOutput(size);
  }

  @Override
  public Output newBufferedOutput(boolean direct, int size) {
    return delegate.newBufferedOutput(direct, size);
  }

  @Override
  public Output newCompositeOutput() {
    return delegate.newCompositeOutput();
  }

  @Override
  public Output wrap(@NonNull ByteBuffer buffer) {
    return delegate.wrap(buffer);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes) {
    return delegate.wrap(bytes);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return delegate.wrap(bytes, offset, length);
  }
}
