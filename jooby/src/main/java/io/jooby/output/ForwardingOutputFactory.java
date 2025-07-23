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
  public OutputOptions getOptions() {
    return delegate.getOptions();
  }

  @Override
  public OutputFactory setOptions(@NonNull OutputOptions options) {
    delegate.setOptions(options);
    return this;
  }

  @Override
  public BufferedOutput allocate(int size) {
    return delegate.allocate(size);
  }

  @Override
  public BufferedOutput allocate(boolean direct, int size) {
    return delegate.allocate(direct, size);
  }

  @Override
  public BufferedOutput newComposite() {
    return delegate.newComposite();
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
