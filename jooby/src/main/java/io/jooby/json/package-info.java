/**
 * Provides the core JSON processing contracts and abstractions for the Jooby framework.
 *
 * <p>This package defines the foundational interfaces (such as {@link io.jooby.json.JsonEncoder},
 * {@link io.jooby.json.JsonDecoder}, and {@link io.jooby.json.JsonCodec}) that allow Jooby to
 * integrate seamlessly with various external JSON libraries (like Jackson, Gson, or Moshi). By
 * implementing these contracts, those libraries can participate in Jooby's content negotiation,
 * enabling automatic serialization of HTTP responses and deserialization of HTTP request bodies.
 *
 * <h2>Null-Safety Guarantee</h2>
 *
 * <p>This package is explicitly marked with {@link org.jspecify.annotations.NullMarked}. This
 * establishes a strict nullability contract where all types (parameters, return types, and fields)
 * within this package are considered <strong>non-null by default</strong>, unless explicitly
 * annotated otherwise (e.g., using {@code @Nullable}).
 *
 * <p>Adopting JSpecify semantics ensures excellent interoperability with null-safe languages like
 * Kotlin and provides robust guarantees for modern static code analysis tools.
 *
 * <p><strong>Important Note:</strong> Jooby core itself <em>does not</em> implement these
 * interfaces. These contracts act as a bridge and are designed to be implemented exclusively by
 * dedicated JSON modules (such as {@code jooby-jackson}, {@code jooby-gson}, or {@code
 * jooby-avaje-json}), etc.
 *
 * @see io.jooby.json.JsonCodec
 * @author edgar
 * @since 4.5.0
 */
@org.jspecify.annotations.NullMarked
package io.jooby.json;
