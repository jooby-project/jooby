/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Value;

/**
 * Value converter for values that come from config, query, path, form, path parameters into more
 * specific type.
 *
 * @author edgar.
 * @since 4.0.0
 */
public interface Converter {
  /**
   * Convert to specific type.
   *
   * @param type Requested type.
   * @param value Value value.
   * @return Converted value.
   */
  Object convert(@NonNull Type type, @NonNull Value value);
}
