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
  public OutputFactory setOptions(OutputOptions options) {
    delegate.setOptions(options);
    return this;
  }

  @Override
  public Output newOutput(int size) {
    return delegate.newOutput(size);
  }

  @Override
  public Output newOutput(boolean direct, int size) {
    return delegate.newOutput(direct, size);
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
