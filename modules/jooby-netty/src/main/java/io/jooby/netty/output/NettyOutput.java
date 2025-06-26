/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntPredicate;

import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;

public interface NettyOutput extends Output {
  ByteBuf byteBuf();

  @Override
  default Iterator<ByteBuffer> split(IntPredicate predicate) {
    var buffer = byteBuf();
    var offset = buffer.readerIndex();
    var chunks = new ArrayList<ByteBuffer>();
    for (int i = offset; i < size(); i++) {
      var b = buffer.getByte(i);
      if (predicate.test(b)) {
        chunks.add(buffer.retainedSlice(offset, i + 1).nioBuffer());
        offset = i + 1;
      }
    }
    if (offset < size()) {
      chunks.add(buffer.retainedSlice(offset, size()).nioBuffer());
    }
    return chunks.iterator();
  }
}
