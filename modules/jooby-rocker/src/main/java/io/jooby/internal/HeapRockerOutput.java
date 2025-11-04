/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.nio.charset.Charset;
import java.util.Arrays;

import com.fizzed.rocker.ContentType;
import com.fizzed.rocker.RockerOutputFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.rocker.BufferedRockerOutput;

/**
 * Rocker output that uses a byte array to render the output. It uses a thread-local cache.
 *
 * @author edgar
 */
public class HeapRockerOutput implements BufferedRockerOutput {
  private static final ThreadLocal<HeapRockerOutput> TL = new ThreadLocal<>();

  private static final int BUFFER_SIZE = 4096;

  /**
   * The maximum size of array to allocate. Some VMs reserve some header words in an array. Attempts
   * to allocate larger arrays may result in OutOfMemoryError: Requested array size exceeds VM limit
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  private final OutputFactory factory;

  private final Charset charset;

  private final ContentType contentType;

  /** The buffer where data is stored. */
  protected byte[] buf;

  /** The number of valid bytes in the buffer. */
  protected int count;

  HeapRockerOutput(
      OutputFactory factory, Charset charset, ContentType contentType, int bufferSize) {
    this.factory = factory;
    this.charset = charset;
    this.buf = new byte[bufferSize];
    this.contentType = contentType;
  }

  HeapRockerOutput reset() {
    count = 0;
    return this;
  }

  @Override
  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public Charset getCharset() {
    return charset;
  }

  @Override
  public HeapRockerOutput w(String string) {
    return w(string.getBytes(charset));
  }

  @Override
  public HeapRockerOutput w(byte[] bytes) {
    int len = bytes.length;
    ensureCapacity(count + len);
    System.arraycopy(bytes, 0, buf, count, len);
    count += len;
    return this;
  }

  @Override
  public int getByteLength() {
    return count;
  }

  /**
   * Get a view of the byte buffer.
   *
   * @return Byte buffer.
   */
  public @NonNull Output toOutput() {
    return factory.wrap(buf, 0, count);
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

  public static RockerOutputFactory<BufferedRockerOutput> factory(
      Charset charset, OutputFactory factory) {
    return (contentType, charsetName) -> {
      var output = TL.get();
      if (output == null) {
        output = new HeapRockerOutput(factory, charset, contentType, BUFFER_SIZE);
        TL.set(output);
      }
      output.reset();
      return output;
    };
  }
}
