/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import io.jooby.Value;

/**
 * Instructs how a {@link Value} must be converted to the requested type. The hint is applied on all
 * the built-in converters. Custom converters might or might not follow the conversion hint.
 *
 * @author edgar
 * @since 4.0.0
 */
public enum ConversionHint {
  /**
   * Always produces an instance of the required type and make sure at least one value matches the
   * output type as property or constructor argument. If nothing matches a {@link
   * io.jooby.exception.TypeMismatchException} will be thrown.
   */
  Strict,
  /**
   * If no value matches the output type property or constructor argument, this produces a <code>
   * null</code> output.
   */
  Nullable,
  /**
   * Produces an instance of the required type even if no value matches the output property or
   * constructor argument.
   */
  Empty,
}
