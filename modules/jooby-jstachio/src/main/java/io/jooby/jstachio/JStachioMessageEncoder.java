/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;
import java.util.function.BiFunction;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.buffer.BufferedOutput;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.output.ByteBufferEncodedOutput;

class JStachioMessageEncoder extends JStachioRenderer<BufferedOutput> implements MessageEncoder {

  public JStachioMessageEncoder(
      JStachio jstachio,
      JStachioBuffer buffer,
      BiFunction<Context, String, String> contextFunction) {
    super(jstachio, buffer, contextFunction);
  }

  @Override
  public BufferedOutput encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    if (supportsType(value.getClass())) {
      return render(ctx, value);
    }
    return null;
  }

  @Override
  BufferedOutput extractOutput(Context ctx, ByteBufferEncodedOutput stream) throws IOException {
    return ctx.getOutputFactory().wrap(stream.asByteBuffer());
  }
}
