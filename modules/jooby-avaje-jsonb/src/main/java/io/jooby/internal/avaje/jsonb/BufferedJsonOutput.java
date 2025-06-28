/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.jsonb;

import java.io.IOException;
import java.io.OutputStream;

import io.avaje.json.stream.JsonOutput;
import io.jooby.output.BufferedOutput;

public class BufferedJsonOutput implements JsonOutput {
  private final BufferedOutput output;

  public BufferedJsonOutput(BufferedOutput output) {
    this.output = output;
  }

  @Override
  public void write(byte[] content, int offset, int length) throws IOException {
    output.write(content, offset, length);
  }

  @Override
  public void flush() throws IOException {
    // NOOP
  }

  @Override
  public OutputStream unwrapOutputStream() {
    return output.asOutputStream();
  }

  @Override
  public void close() throws IOException {
    // NOOP
  }
}
