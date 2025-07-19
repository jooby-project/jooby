/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.DataBuffer;
import io.jooby.exception.NotAcceptableException;

/**
 * Render a route output as byte array.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface MessageEncoder {

  /** To string renderer. */
  MessageEncoder TO_STRING =
      (ctx, value) -> {
        if (ctx.accept(ctx.getResponseType())) {
          return ctx.getBufferFactory().wrap(value.toString().getBytes(StandardCharsets.UTF_8));
        }
        throw new NotAcceptableException(ctx.header("Accept").valueOrNull());
      };

  /**
   * Encodes a value into a buffer or <code>null</code> if a given object isn't supported it.
   *
   * @param ctx Web context.
   * @param value Value to render.
   * @return Encoded value or <code>null</code> if given object isn't supported it.
   * @throws Exception If something goes wrong.
   */
  @Nullable DataBuffer encode(@NonNull Context ctx, @NonNull Object value) throws Exception;
}
