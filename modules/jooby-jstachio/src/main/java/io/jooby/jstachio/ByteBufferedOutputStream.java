/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This is basically the same as Rockers byte buffer but as an OutputStream because JStachio wants
 * that interface. Currently it is internal.
 *
 * @author agentgt
 */
class ByteBufferedOutputStream extends OutputStream {

  /** Default buffer size: <code>4k</code>. */
  public static final int BUFFER_SIZE = 4096;

  /**
   * The maximum size of array to allocate. Some VMs reserve some header words in an array. Attempts
   * to allocate larger arrays may result in OutOfMemoryError: Requested array size exceeds VM limit
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  /** The buffer where data is stored. */
  protected byte[] buf;

  /** The number of valid bytes in the buffer. */
  protected int count;

  ByteBufferedOutputStream(int bufferSize) {
    this.buf = new byte[bufferSize];
  }

  void reset() {
    count = 0;
  }

  @Override
  public void close() {
    this.reset();
  }

  @Override
  public void write(byte[] bytes) {
    int len = bytes.length;
    ensureCapacity(count + len);
    System.arraycopy(bytes, 0, buf, count, len);
    count += len;
  }

  public int size() {
    return count;
  }

  /**
   * Copy internal byte array into a new array.
   *
   * @return Byte array.
   */
  public byte[] toByteArray() {
    byte[] array = new byte[count];
    System.arraycopy(buf, 0, array, 0, count);
    return array;
  }

  /**
   * Get a view of the byte buffer.
   *
   * @return Byte buffer.
   */
  public ByteBuffer toBuffer() {
    return ByteBuffer.wrap(buf, 0, count);
  }

  private void ensureCapacity(int minCapacity) {
    // overflow-conscious code
    if (minCapacity - buf.length > 0) {
      grow(minCapacity);
    }
  }

  /**
   * Increases the capacity to ensure that it can hold at least the number of elements specified by
   * the minimum capacity argument.
   *
   * @param minCapacity the desired minimum capacity
   */
  private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = buf.length;
    int newCapacity = oldCapacity << 1;
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
      newCapacity = hugeCapacity(minCapacity);
    }
    buf = Arrays.copyOf(buf, newCapacity);
  }

  private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) {
      throw new OutOfMemoryError();
    }
    return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
  }

  @Override
  public void write(int b) {
    throw new UnsupportedOperationException("expecting only write(byte[])");
  }
}
