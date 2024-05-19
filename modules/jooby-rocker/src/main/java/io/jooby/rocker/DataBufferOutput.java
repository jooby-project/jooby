/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.nio.charset.Charset;

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
public class DataBufferOutput implements RockerOutput<DataBufferOutput> {

  /** Default buffer size: <code>4k</code>. */
  public static final int BUFFER_SIZE = 4096;

  private final Charset charset;
  private final ContentType contentType;

  /** The buffer where data is stored. */
  protected DataBuffer buffer;

  DataBufferOutput(Charset charset, ContentType contentType, DataBuffer buffer) {
    this.charset = charset;
    this.contentType = contentType;
    this.buffer = buffer;
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
  public DataBufferOutput w(String string) {
    buffer.write(string, getCharset());
    return this;
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

  static RockerOutputFactory<DataBufferOutput> factory(
      Charset charset, DataBufferFactory factory, int bufferSize) {
    return (contentType, charsetName) ->
        new DataBufferOutput(charset, contentType, factory.allocateBuffer(bufferSize));
  }
}
