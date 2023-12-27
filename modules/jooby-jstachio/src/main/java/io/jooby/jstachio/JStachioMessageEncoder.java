/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;
import java.util.function.BiFunction;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.output.ByteBufferEncodedOutput;

class JStachioMessageEncoder extends JStachioRenderer<byte[]> implements MessageEncoder {

  public JStachioMessageEncoder(
      JStachio jstachio,
      JStachioBuffer buffer,
      BiFunction<Context, String, String> contextFunction) {
    super(jstachio, buffer, contextFunction);
  }

  @Override
  public byte[] encode(Context ctx, Object value) throws Exception {
    if (supportsType(value.getClass())) {
      return render(ctx, value);
    }
    return null;
  }

  @Override
  byte[] extractOutput(Context ctx, ByteBufferEncodedOutput stream) throws IOException {
    return stream.toByteArray();
  }
}
