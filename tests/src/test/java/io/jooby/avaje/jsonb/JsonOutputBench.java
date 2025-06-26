/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.io.IOException;
import java.io.OutputStream;

import io.avaje.json.stream.JsonOutput;
import io.jooby.output.Output;

public class JsonOutputBench implements JsonOutput {

  private final Output output;

  public JsonOutputBench(Output output) {
    this.output = output;
  }

  @Override
  public void write(byte[] content, int offset, int length) throws IOException {
    output.write(content, offset, length);
  }

  @Override
  public void flush() {}

  @Override
  public OutputStream unwrapOutputStream() {
    return this.output.asOutputStream();
  }

  @Override
  public void close() {}
}
