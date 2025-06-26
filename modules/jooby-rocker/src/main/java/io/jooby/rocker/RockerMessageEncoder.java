/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.MessageEncoder;
import io.jooby.output.Output;

class RockerMessageEncoder implements MessageEncoder {
  private final RockerOutputFactory<DataBufferOutput> factory;

  RockerMessageEncoder(RockerOutputFactory<DataBufferOutput> factory) {
    this.factory = factory;
  }

  @Override
  public Output encode(@NonNull Context ctx, @NonNull Object value) {
    if (value instanceof RockerModel template) {
      var output = template.render(factory);
      ctx.setResponseLength(output.getByteLength());
      ctx.setDefaultResponseType(MediaType.html);
      return output.toBuffer();
    }
    return null;
  }
}
