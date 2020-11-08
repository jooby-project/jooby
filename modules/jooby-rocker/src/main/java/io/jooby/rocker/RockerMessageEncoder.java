/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import javax.annotation.Nonnull;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;

class RockerMessageEncoder implements MessageEncoder {
  private final RockerOutputFactory<ByteBufferOutput> factory;

  RockerMessageEncoder(RockerOutputFactory<ByteBufferOutput> factory) {
    this.factory = factory;
  }

  @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) {
    if (value instanceof RockerModel) {
      RockerModel template = (RockerModel) value;
      ByteBufferOutput output = template.render(factory);
      ctx.setResponseLength(output.getByteLength());
      ctx.setDefaultResponseType(MediaType.html);
      return output.toByteArray();
    }
    return null;
  }
}
