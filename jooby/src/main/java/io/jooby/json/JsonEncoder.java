/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

/**
 * Contract for encoding (serializing) Java objects into JSON strings.
 *
 * <p><strong>Important Note:</strong> Jooby core itself <em>does not</em> implement these
 * interfaces. These contracts act as a bridge and are designed to be implemented exclusively by
 * dedicated JSON modules (such as {@code jooby-jackson}, {@code jooby-gson}, or {@code
 * jooby-avaje-json}), etc.
 *
 * @since 4.5.0
 * @author edgar
 */
@FunctionalInterface
public interface JsonEncoder {

  /**
   * Encodes a Java object into its JSON string representation.
   *
   * <p>This method takes an arbitrary Java object and converts it into a valid JSON payload.
   * Implementations are responsible for handling the specific serialization rules, configurations,
   * and exception management of their underlying JSON library.
   *
   * @param value The Java object to serialize. This can be a simple data type, a collection, or a
   *     complex custom bean.
   * @return The JSON string representation of the provided object.
   */
  String encode(Object value);
}
