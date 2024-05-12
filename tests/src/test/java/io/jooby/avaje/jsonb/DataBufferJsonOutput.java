/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.io.IOException;
import java.io.OutputStream;

import io.avaje.jsonb.stream.JsonOutput;
import io.jooby.buffer.DataBuffer;

public class DataBufferJsonOutput implements JsonOutput {

  private DataBuffer buffer;

  public DataBufferJsonOutput(DataBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void write(byte[] content, int offset, int length) throws IOException {
    buffer.write(content, offset, length);
  }

  @Override
  public void flush() throws IOException {}

  @Override
  public OutputStream unwrapOutputStream() {
    return this.buffer.asOutputStream();
  }

  @Override
  public void close() throws IOException {}
}
