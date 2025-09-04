/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.netty.buffer.ByteBuf;

public record NettyWrappedOutput(ByteBuf buffer) implements NettyByteBufRef {

  @Override
  public int size() {
    return buffer.readableBytes();
  }

  @NonNull public ByteBuf byteBuf() {
    return buffer;
  }
}
