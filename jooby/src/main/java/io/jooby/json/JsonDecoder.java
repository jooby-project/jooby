/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

import java.lang.reflect.Type;

import io.jooby.Reified;

/**
 * Contract for decoding (deserializing) JSON strings into Java objects.
 *
 * <p>This functional interface provides the core deserialization strategy for Jooby. It is designed
 * to be implemented by specific JSON library integrations (such as Jackson, Gson, Moshi, etc.) to
 * adapt their internal parsing mechanics to Jooby's standard architecture.
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
public interface JsonDecoder {

  /**
   * Decodes a JSON string into the specified Java {@link java.lang.reflect.Type}.
   *
   * <p>This is the primary decoding method that all underlying JSON libraries must implement. It
   * accepts a raw reflection {@code Type}, making it capable of handling both simple classes and
   * complex parameterized/generic types (e.g., {@code List<MyObject>}).
   *
   * @param json The JSON payload as a string.
   * @param type The target Java reflection type to deserialize into.
   * @return The deserialized Java object instance.
   * @param <T> The expected generic type of the returned object.
   */
  <T> T decode(String json, Type type);

  /**
   * Decodes a JSON string into the specified Java {@link Class}.
   *
   * <p>This is a convenience method for deserializing simple, non-generic types. It delegates
   * directly to {@link #decode(String, Type)}.
   *
   * @param json The JSON payload as a string.
   * @param type The target Java class to deserialize into.
   * @return The deserialized Java object instance.
   * @param <T> The expected generic type of the returned object.
   */
  default <T> T decode(String json, Class<T> type) {
    return decode(json, (java.lang.reflect.Type) type);
  }

  /**
   * Decodes a JSON string into the specified Jooby {@link Reified} type.
   *
   * <p>This is a convenience method for deserializing complex, generic types while avoiding type
   * erasure. It extracts the underlying reflection type from the {@code Reified} token and
   * delegates to {@link #decode(String, Type)}.
   *
   * @param json The JSON payload as a string.
   * @param type The Reified type token capturing the target type.
   * @return The deserialized Java object instance.
   * @param <T> The expected generic type of the returned object.
   */
  default <T> T decode(String json, Reified<T> type) {
    return decode(json, type.getType());
  }
}
