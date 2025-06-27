/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;

public class ByteBufferOutput implements Output {
  private static final int MAX_CAPACITY = Integer.MAX_VALUE;

  private static final int CAPACITY_THRESHOLD = 1024 * 1024 * 4;

  private ByteBuffer buffer;

  private int capacity;

  private int readPosition;

  private int writePosition;

  public ByteBufferOutput(boolean direct, int capacity) {
    this.buffer = allocate(capacity, direct);
    this.capacity = this.buffer.remaining();
  }

  public ByteBufferOutput(boolean direct) {
    this(direct, BUFFER_SIZE);
  }

  public ByteBufferOutput(int bufferSize) {
    this(false, bufferSize);
  }

  public ByteBufferOutput() {
    this(BUFFER_SIZE);
  }

  @Override
  public int size() {
    return this.writePosition - this.readPosition;
  }

  private void ensureWritable(int length) {
    if (length > writableByteCount()) {
      int newCapacity = calculateCapacity(this.writePosition + length);
      setCapacity(newCapacity);
    }
  }

  @Override
  public void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  public @NonNull Iterator<ByteBuffer> iterator() {
    return List.of(asByteBuffer()).iterator();
  }

  private int writableByteCount() {
    return this.capacity - this.writePosition;
  }

  @Override
  public @NonNull ByteBuffer asByteBuffer() {
    return this.buffer.duplicate().position(this.readPosition).limit(this.writePosition);
  }

  @Override
  public String asString(@NonNull Charset charset) {
    return charset.decode(asByteBuffer()).toString();
  }

  @Override
  public Output write(byte b) {
    ensureWritable(1);
    this.buffer.put(this.writePosition, b);
    this.writePosition += 1;
    return this;
  }

  @Override
  public Output write(byte[] source) {
    return write(source, 0, source.length);
  }

  @Override
  public Output write(byte[] source, int offset, int length) {
    ensureWritable(length);

    var tmp = this.buffer.duplicate();
    int limit = this.writePosition + length;
    tmp.clear().position(this.writePosition).limit(limit);
    tmp.put(source, offset, length);

    this.writePosition += length;
    return this;
  }

  @Override
  public Output write(@NonNull ByteBuffer source) {
    ensureWritable(source.remaining());
    var length = source.remaining();
    var tmp = this.buffer.duplicate();
    var limit = this.writePosition + source.remaining();
    tmp.clear().position(this.writePosition).limit(limit);
    tmp.put(source);
    this.writePosition += length;
    return this;
  }

  @Override
  public Output clear() {
    this.buffer.clear();
    return this;
  }

  @Override
  public void send(Context ctx) {
    ctx.send(asByteBuffer());
  }

  @Override
  public String toString() {
    return "readPosition="
        + this.readPosition
        + ", writePosition="
        + this.writePosition
        + ", size="
        + this.size()
        + ", capacity="
        + this.capacity;
  }

  /** Calculate the capacity of the buffer. */
  private int calculateCapacity(int neededCapacity) {
    if (neededCapacity == CAPACITY_THRESHOLD) {
      return CAPACITY_THRESHOLD;
    } else if (neededCapacity > CAPACITY_THRESHOLD) {
      int newCapacity = neededCapacity / CAPACITY_THRESHOLD * CAPACITY_THRESHOLD;
      if (newCapacity > MAX_CAPACITY - CAPACITY_THRESHOLD) {
        newCapacity = MAX_CAPACITY;
      } else {
        newCapacity += CAPACITY_THRESHOLD;
      }
      return newCapacity;
    } else {
      int newCapacity = 64;
      while (newCapacity < neededCapacity) {
        newCapacity <<= 1;
      }
      return Math.min(newCapacity, MAX_CAPACITY);
    }
  }

  private void setCapacity(int newCapacity) {
    if (newCapacity < 0) {
      throw new IllegalArgumentException(
          String.format("'newCapacity' %d must be 0 or higher", newCapacity));
    }
    var readPosition = this.readPosition;
    var writePosition = this.writePosition;
    var oldCapacity = this.capacity;

    if (newCapacity > oldCapacity) {
      var oldBuffer = this.buffer;
      var newBuffer = allocate(newCapacity, oldBuffer.isDirect());
      oldBuffer.position(0).limit(oldBuffer.capacity());
      newBuffer.position(0).limit(oldBuffer.capacity());
      newBuffer.put(oldBuffer);
      newBuffer.clear();
      setNativeBuffer(newBuffer);
    } else if (newCapacity < oldCapacity) {
      var oldBuffer = this.buffer;
      var newBuffer = allocate(newCapacity, oldBuffer.isDirect());
      if (readPosition < newCapacity) {
        if (writePosition > newCapacity) {
          writePosition = newCapacity;
          this.writePosition = writePosition;
        }
        oldBuffer.position(readPosition).limit(writePosition);
        newBuffer.position(readPosition).limit(writePosition);
        newBuffer.put(oldBuffer);
        newBuffer.clear();
      } else {
        this.readPosition = newCapacity;
        this.writePosition = newCapacity;
      }
      setNativeBuffer(newBuffer);
    }
  }

  private void setNativeBuffer(ByteBuffer buffer) {
    this.buffer = buffer;
    this.capacity = buffer.capacity();
  }

  private static ByteBuffer allocate(int capacity, boolean direct) {
    return direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
  }
}
