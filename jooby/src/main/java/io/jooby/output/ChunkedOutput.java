/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.util.List;

public interface ChunkedOutput extends Output {
  List<ByteBuffer> getChunks();
}
