/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.FlashMapImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Flash scope.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface FlashMap extends Map<String, String> {

  String NAME = "flash";

  /**
   * Creates a new flash-scope using the given cookie.
   *
   * @param ctx
   * @param template
   * @return
   */
  static @Nonnull FlashMap create(Context ctx, Cookie template) {
    return new FlashMapImpl(ctx, template);
  }

  /**
   * Keep flash cookie for next request.
   */
  FlashMap keep();

}
