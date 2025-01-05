/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.IntPredicate;

import io.jooby.Context;

/**
 * Provides a convenient implementation of the {@link DataBuffer} interface that can be overridden
 * to adapt the delegate.
 *
 * <p>These methods default to calling through to the wrapped delegate object.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public class DataBufferWrapper implements DataBuffer {

  private final DataBuffer delegate;

  /**
   * Create a new {@code DataBufferWrapper} that wraps the given buffer.
   *
   * @param delegate the buffer to wrap
   */
  public DataBufferWrapper(DataBuffer delegate) {
    Assert.notNull(delegate, "Delegate must not be null");
    this.delegate = delegate;
  }

  /** Return the wrapped delegate. */
  public DataBuffer dataBuffer() {
    return this.delegate;
  }

  @Override
  public DataBufferFactory factory() {
    return this.delegate.factory();
  }

  @Override
  public int indexOf(IntPredicate predicate, int fromIndex) {
    return this.delegate.indexOf(predicate, fromIndex);
  }

  @Override
  public int lastIndexOf(IntPredicate predicate, int fromIndex) {
    return this.delegate.lastIndexOf(predicate, fromIndex);
  }

  @Override
  public int readableByteCount() {
    return this.delegate.readableByteCount();
  }

  @Override
  public int writableByteCount() {
    return this.delegate.writableByteCount();
  }

  @Override
  public int capacity() {
    return this.delegate.capacity();
  }

  @Override
  public DataBuffer duplicate() {
    return new DataBufferWrapper(this.delegate.duplicate());
  }

  @Override
  public DataBuffer ensureWritable(int capacity) {
    this.delegate.ensureWritable(capacity);
    return this;
  }

  @Override
  public int readPosition() {
    return this.delegate.readPosition();
  }

  @Override
  public DataBuffer readPosition(int readPosition) {
    this.delegate.readPosition(readPosition);
    return this;
  }

  @Override
  public int writePosition() {
    return this.delegate.writePosition();
  }

  @Override
  public DataBuffer writePosition(int writePosition) {
    this.delegate.writePosition(writePosition);
    return this;
  }

  @Override
  public byte getByte(int index) {
    return this.delegate.getByte(index);
  }

  @Override
  public byte read() {
    return this.delegate.read();
  }

  @Override
  public DataBuffer read(byte[] destination) {
    this.delegate.read(destination);
    return this;
  }

  @Override
  public DataBuffer read(byte[] destination, int offset, int length) {
    this.delegate.read(destination, offset, length);
    return this;
  }

  @Override
  public DataBuffer write(byte b) {
    this.delegate.write(b);
    return this;
  }

  @Override
  public DataBuffer write(byte[] source) {
    this.delegate.write(source);
    return this;
  }

  @Override
  public DataBuffer write(byte[] source, int offset, int length) {
    this.delegate.write(source, offset, length);
    return this;
  }

  @Override
  public DataBuffer write(DataBuffer... buffers) {
    this.delegate.write(buffers);
    return this;
  }

  @Override
  public DataBuffer write(ByteBuffer... buffers) {
    this.delegate.write(buffers);
    return this;
  }

  @Override
  public DataBuffer write(CharSequence charSequence, Charset charset) {
    this.delegate.write(charSequence, charset);
    return this;
  }

  @Override
  public DataBuffer split(int index) {
    this.delegate.split(index);
    return this;
  }

  @Override
  public void toByteBuffer(ByteBuffer dest) {
    this.delegate.toByteBuffer(dest);
  }

  @Override
  public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
    this.delegate.toByteBuffer(srcPos, dest, destPos, length);
  }

  @Override
  public ByteBufferIterator readableByteBuffers() {
    return this.delegate.readableByteBuffers();
  }

  @Override
  public ByteBufferIterator writableByteBuffers() {
    return this.delegate.writableByteBuffers();
  }

  @Override
  public InputStream asInputStream() {
    return this.delegate.asInputStream();
  }

  @Override
  public InputStream asInputStream(boolean releaseOnClose) {
    return this.delegate.asInputStream(releaseOnClose);
  }

  @Override
  public OutputStream asOutputStream() {
    return this.delegate.asOutputStream();
  }

  @Override
  public String toString(Charset charset) {
    return this.delegate.toString(charset);
  }

  @Override
  public DataBuffer clear() {
    delegate.clear();
    return this;
  }

  @Override
  public Context send(Context ctx) {
    this.delegate.send(ctx);
    return ctx;
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    return this.delegate.toString(index, length, charset);
  }
}
