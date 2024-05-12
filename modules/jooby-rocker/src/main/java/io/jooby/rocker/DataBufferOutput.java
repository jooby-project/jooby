/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fizzed.rocker.ContentType;
import com.fizzed.rocker.RockerOutput;
import com.fizzed.rocker.RockerOutputFactory;
import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DataBufferFactory;

/**
 * Rocker output that uses a byte array to render the output.
 *
 * @author edgar
 */
class DataBufferOutput implements RockerOutput<DataBufferOutput> {

  /** Default buffer size: <code>4k</code>. */
  public static final int BUFFER_SIZE = 4096;

  private final ContentType contentType;

  /** The buffer where data is stored. */
  protected DataBuffer buffer;

  DataBufferOutput(ContentType contentType, DataBuffer buffer) {
    this.contentType = contentType;
    this.buffer = buffer;
  }

  void reset() {
    buffer.clear();
  }

  @Override
  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public DataBufferOutput w(String string) {
    return w(string.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public DataBufferOutput w(byte[] bytes) {
    buffer.write(bytes);
    return this;
  }

  @Override
  public int getByteLength() {
    return buffer.readableByteCount();
  }

  /**
   * Get a view of the byte buffer.
   *
   * @return Byte buffer.
   */
  public DataBuffer toBuffer() {
    return buffer;
  }

  static RockerOutputFactory<DataBufferOutput> factory(DataBufferFactory factory, int bufferSize) {
    return (contentType, charsetName) ->
        new DataBufferOutput(contentType, factory.allocateBuffer(bufferSize));
  }

  static RockerOutputFactory<DataBufferOutput> reuse(
      RockerOutputFactory<DataBufferOutput> factory) {
    return new RockerOutputFactory<DataBufferOutput>() {
      private final ThreadLocal<DataBufferOutput> thread = new ThreadLocal<>();

      @Override
      public DataBufferOutput create(ContentType contentType, String charsetName) {
        DataBufferOutput output = thread.get();
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
