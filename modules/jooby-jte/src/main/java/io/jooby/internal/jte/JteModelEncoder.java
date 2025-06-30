/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jte;

import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import gg.jte.models.runtime.JteModel;
import io.jooby.Context;
import io.jooby.buffer.BufferedOutput;

public class JteModelEncoder implements io.jooby.MessageEncoder {
  @Nullable @Override
  public BufferedOutput encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    if (value instanceof JteModel jte) {
      var buffer = ctx.getOutputFactory().newBufferedOutput();
      jte.render(new BufferedTemplateOutput(buffer, StandardCharsets.UTF_8));
      return buffer;
    }
    return null;
  }
}
