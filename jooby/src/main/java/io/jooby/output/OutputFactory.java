/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface OutputFactory {
  Output newBufferedOutput(int size);

  default Output newBufferedOutput() {
    return newBufferedOutput(Output.BUFFER_SIZE);
  }

  default Output wrap(String value) {
    return wrap(value, StandardCharsets.UTF_8);
  }

  default Output wrap(String value, Charset charset) {
    return wrap(value.getBytes(charset));
  }

  Output wrap(ByteBuffer buffer);

  Output wrap(byte[] bytes);

  Output wrap(byte[] bytes, int offset, int length);

  ChunkedOutput newChunkedOutput();
}
