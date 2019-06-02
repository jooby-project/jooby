/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * Render a route output as byte array.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Renderer {

  /** To string renderer. */
  Renderer TO_STRING = (ctx, value) -> {
    if (ctx.accept(ctx.getResponseType())) {
      return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    throw new StatusCodeException(StatusCode.NOT_ACCEPTABLE);
  };

  /**
   * Renderer a value into a byte array or <code>null</code> if given object isn't supported it.
   *
   * @param ctx Web context.
   * @param value Value to render.
   * @return Value as byte array or <code>null</code> if given object isn't supported it.
   * @throws Exception If something goes wrong.
   */
  @Nonnull byte[] render(@Nonnull Context ctx, @Nonnull Object value) throws Exception;

  /**
   * Execute this renderer only if the <code>Accept</code> header matches the content-type
   * parameter.
   *
   * @param contentType Mediatype to test.
   * @return A new renderer with accept header matching.
   */
  @Nonnull default Renderer accept(@Nonnull MediaType contentType) {
    return (ctx, value) -> {
      if (ctx.accept(contentType)) {
        return render(ctx, value);
      }
      return null;
    };
  }

}
