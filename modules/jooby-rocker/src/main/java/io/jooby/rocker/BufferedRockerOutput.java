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
import io.jooby.output.BufferedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

/**
 * Rocker output that uses a byte array to render the output.
 *
 * @author edgar
 */
public class BufferedRockerOutput implements RockerOutput<BufferedRockerOutput> {

  /** Default buffer size: <code>4k</code>. */
  public static final int BUFFER_SIZE = 4096;

  private final Charset charset;
  private final ContentType contentType;

  /** The buffer where data is stored. */
  protected BufferedOutput output;

  BufferedRockerOutput(Charset charset, ContentType contentType, BufferedOutput output) {
    this.charset = charset;
    this.contentType = contentType;
    this.output = output;
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
  public BufferedRockerOutput w(String string) {
    output.write(string, getCharset());
    return this;
  }

  @Override
  public BufferedRockerOutput w(byte[] bytes) {
    output.write(bytes);
    return this;
  }

  @Override
  public int getByteLength() {
    return output.size();
  }

  /**
   * Get a view of the byte buffer.
   *
   * @return Byte buffer.
   */
  public Output asOutput() {
    return output;
  }

  static RockerOutputFactory<BufferedRockerOutput> factory(Charset charset, OutputFactory factory) {
    return (contentType, charsetName) ->
        new BufferedRockerOutput(charset, contentType, factory.allocate());
  }
}
