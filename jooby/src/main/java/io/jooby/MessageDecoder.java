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
public interface MessageDecoder {

  /**
   * Resolve parsing as {@link StatusCode#UNSUPPORTED_MEDIA_TYPE}.
   */
  MessageDecoder UNSUPPORTED_MEDIA_TYPE = new MessageDecoder() {
    @Override public <T> T decode(Context ctx, Type type) {
      throw new StatusCodeException(StatusCode.UNSUPPORTED_MEDIA_TYPE);
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
  @Nonnull <T> T decode(@Nonnull Context ctx, @Nonnull Type type) throws Exception;
}
