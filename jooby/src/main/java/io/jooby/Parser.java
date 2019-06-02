/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * Parse HTTP body into a target type.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface Parser {

  /**
   * Resolve parsing as {@link StatusCode#UNSUPPORTED_MEDIA_TYPE}.
   */
  Parser UNSUPPORTED_MEDIA_TYPE = new Parser() {
    @Override public <T> T parse(Context ctx, Type type) {
      throw new StatusCodeException(StatusCode.UNSUPPORTED_MEDIA_TYPE);
    }
  };
  /**
   * Parse body to one of the <code>raw</code> types: String, byte[], etc.
   */
  Parser RAW = new Parser() {
    @Override public <T> T parse(Context ctx, Type type) {
      return ctx.body().to(type);
    }
  };

  /**
   * Parse HTTP body into the given type.
   *
   * @param ctx Web context.
   * @param type Target/expected type.
   * @param <T> Dynamic binding of the target type.
   * @return An instance of the target type.
   * @throws Exception Is something goes wrong.
   */
  @Nonnull <T> T parse(@Nonnull Context ctx, @Nonnull Type type) throws Exception;
}
