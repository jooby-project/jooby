/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fizzed.rocker.ContentType;
import com.fizzed.rocker.RockerOutput;
import com.fizzed.rocker.RockerOutputFactory;

/**
 * Rocker output that uses a byte array to render the output.
 *
 * @author edgar
 */
public class ByteBufferOutput implements RockerOutput<ByteBufferOutput> {

  /** Default buffer size: <code>4k</code>. */
  public static final int BUFFER_SIZE = 4096;

  /**
   * The maximum size of array to allocate.
   * Some VMs reserve some header words in an array.
   * Attempts to allocate larger arrays may result in
   * OutOfMemoryError: Requested array size exceeds VM limit
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  private final ContentType contentType;

  /**
   * The buffer where data is stored.
   */
  protected byte[] buf;

  /**
   * The number of valid bytes in the buffer.
   */
  protected int count;

  ByteBufferOutput(ContentType contentType, int bufferSize) {
    this.buf = new byte[bufferSize];
    this.contentType = contentType;
  }

  void reset() {
    count = 0;
  }

  @Override public ContentType getContentType() {
    return contentType;
  }

  @Override public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  @Override public ByteBufferOutput w(String string) {
    return w(string.getBytes(StandardCharsets.UTF_8));
  }

  @Override public ByteBufferOutput w(byte[] bytes) {
    int len = bytes.length;
    ensureCapacity(count + len);
    System.arraycopy(bytes, 0, buf, count, len);
    count += len;
    return this;
  }

  @Override public int getByteLength() {
    return count;
  }

  /**
   * Get a view of the byte buffer.
   *
   * @return Byte buffer.
   */
  public ByteBuffer toBuffer() {
    return ByteBuffer.wrap(buf, 0, count);
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

  private void ensureCapacity(int minCapacity) {
    // overflow-conscious code
    if (minCapacity - buf.length > 0) {
      grow(minCapacity);
    }
  }

  /**
   * Increases the capacity to ensure that it can hold at least the
   * number of elements specified by the minimum capacity argument.
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
    return (minCapacity > MAX_ARRAY_SIZE)
        ? Integer.MAX_VALUE
        : MAX_ARRAY_SIZE;
  }

  static RockerOutputFactory<ByteBufferOutput> factory(int bufferSize) {
    return (contentType, charsetName) -> new ByteBufferOutput(contentType, bufferSize);
  }

  static RockerOutputFactory<ByteBufferOutput> reuse(
      RockerOutputFactory<ByteBufferOutput> factory) {
    return new RockerOutputFactory<ByteBufferOutput>() {
      private final ThreadLocal<ByteBufferOutput> thread = new ThreadLocal<>();

      @Override public ByteBufferOutput create(ContentType contentType, String charsetName) {
        ByteBufferOutput output = thread.get();
        if (output == null) {
          output = factory.create(contentType, charsetName);
          thread.set(output);
        }
        output.reset();
        return output;
      }
    };
  }
}
