/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.FlashMapImpl;

/**
 * Flash map.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface FlashMap extends Map<String, String> {

  /** Flash map attribute. */
  String NAME = "flash";

  /**
   * Creates a new flash-scope using the given cookie.
   *
   * @param ctx Web context.
   * @param template Cookie template.
   * @return A new flash map.
   */
  static @NonNull FlashMap create(@NonNull Context ctx, @NonNull Cookie template) {
    return new FlashMapImpl(ctx, template);
  }

  /**
   * Keep flash cookie for next request.
   *
   * @return This flash map.
   */
  @NonNull FlashMap keep();
}
