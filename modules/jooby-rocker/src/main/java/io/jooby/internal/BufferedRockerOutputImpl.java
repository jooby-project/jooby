/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.nio.charset.Charset;

import com.fizzed.rocker.ContentType;
import com.fizzed.rocker.RockerOutputFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.output.BufferedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.rocker.BufferedRockerOutput;

/**
 * Rocker output that uses a byte array to render the output.
 *
 * @author edgar
 */
public class BufferedRockerOutputImpl implements BufferedRockerOutput {

  public static final int BUFFER_SIZE = 4096;

  private final Charset charset;
  private final ContentType contentType;

  /** The buffer where data is stored. */
  protected BufferedOutput output;

  BufferedRockerOutputImpl(Charset charset, ContentType contentType, BufferedOutput output) {
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
  public BufferedRockerOutputImpl w(String string) {
    output.write(string.getBytes(charset));
    return this;
  }

  @Override
  public BufferedRockerOutputImpl w(byte[] bytes) {
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
  public @NonNull Output toOutput() {
    return output;
  }

  public static RockerOutputFactory<BufferedRockerOutput> factory(
      Charset charset, OutputFactory factory) {
    return (contentType, charsetName) ->
        new BufferedRockerOutputImpl(charset, contentType, factory.allocate(BUFFER_SIZE));
  }
}
