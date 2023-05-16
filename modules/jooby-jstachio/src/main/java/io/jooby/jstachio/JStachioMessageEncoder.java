/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.io.IOException;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.Template;

/*
 * Should this be public?
 */
class JStachioMessageEncoder extends JStachioRenderer<byte[]> implements MessageEncoder {

  public JStachioMessageEncoder(JStachio jstachio, JStachioBuffer buffer) {
    super(jstachio, buffer);
  }

  @Override
  public byte[] encode(Context ctx, Object value) throws Exception {
    if (supportsType(value.getClass())) {
      return render(ctx, value);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  byte[] render(
      Context ctx,
      @SuppressWarnings("rawtypes") Template template,
      Object model,
      ByteBufferedOutputStream stream)
      throws IOException {
    template.write(model, stream);
    return stream.toByteArray();
  }
}
