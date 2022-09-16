/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.exception.NotAcceptableException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * Render a route output as byte array.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface MessageEncoder {

  /** To string renderer. */
  MessageEncoder TO_STRING = (ctx, value) -> {
    if (ctx.accept(ctx.getResponseType())) {
      return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    throw new NotAcceptableException(ctx.header("Accept").valueOrNull());
  };

  /**
   * MessageEncoder a value into a byte array or <code>null</code> if given object isn't supported it.
   *
   * @param ctx Web context.
   * @param value Value to render.
   * @return Value as byte array or <code>null</code> if given object isn't supported it.
   * @throws Exception If something goes wrong.
   */
  @Nullable byte[] encode(@NonNull Context ctx, @NonNull Object value) throws Exception;

}
