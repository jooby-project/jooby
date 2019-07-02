/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;

import javax.annotation.Nonnull;

class RockerMessageEncoder implements MessageEncoder {
  @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof RockerModel) {
      RockerModel template = (RockerModel) value;
      ArrayOfByteArraysOutput output = template.render(ArrayOfByteArraysOutput.FACTORY);
      ctx.setResponseLength(output.getByteLength());
      ctx.setDefaultResponseType(MediaType.html);
      return output.toByteArray();
    }
    return null;
  }
}
