/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

/**
 * DataBuffer implementation created by {@link LeakAwareDataBufferFactory}.
 *
 * @author Arjen Poutsma
 */
class LeakAwareDataBuffer extends DataBufferWrapper implements PooledDataBuffer {

  private final AssertionError leakError;

  private final LeakAwareDataBufferFactory dataBufferFactory;

  LeakAwareDataBuffer(DataBuffer delegate, LeakAwareDataBufferFactory dataBufferFactory) {
    super(delegate);
    Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
    this.dataBufferFactory = dataBufferFactory;
    this.leakError = createLeakError(delegate);
  }

  private static AssertionError createLeakError(DataBuffer delegate) {
    String message =
        String.format(
            "DataBuffer leak detected: {%s} has not been released.%n"
                + "Stack trace of buffer allocation statement follows:",
            delegate);
    AssertionError result = new AssertionError(message);
    // remove first four irrelevant stack trace elements
    StackTraceElement[] oldTrace = result.getStackTrace();
    StackTraceElement[] newTrace = new StackTraceElement[oldTrace.length - 4];
    System.arraycopy(oldTrace, 4, newTrace, 0, oldTrace.length - 4);
    result.setStackTrace(newTrace);
    return result;
  }

  AssertionError leakError() {
    return this.leakError;
  }

  @Override
  public boolean isAllocated() {
    DataBuffer delegate = dataBuffer();
    return delegate instanceof PooledDataBuffer && ((PooledDataBuffer) delegate).isAllocated();
  }

  @Override
  public PooledDataBuffer retain() {
    DataBufferUtils.retain(dataBuffer());
    return this;
  }

  @Override
  public PooledDataBuffer touch(Object hint) {
    DataBufferUtils.touch(dataBuffer(), hint);
    return this;
  }

  @Override
  public boolean release() {
    DataBufferUtils.release(dataBuffer());
    return isAllocated();
  }

  @Override
  public LeakAwareDataBufferFactory factory() {
    return this.dataBufferFactory;
  }

  @Override
  public String toString() {
    return String.format("LeakAwareDataBuffer (%s)", dataBuffer());
  }
}
