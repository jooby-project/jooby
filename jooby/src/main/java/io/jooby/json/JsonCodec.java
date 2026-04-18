/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

/**
 * A unified contract for complete JSON processing, combining both serialization and deserialization
 * capabilities.
 *
 * <p>This interface acts as a convenient composite of {@link JsonEncoder} and {@link JsonDecoder}.
 * Implementations of this interface (such as Jooby's Jackson, Gson, or Moshi integration modules)
 * provide full-stack JSON support. This allows a Jooby application to seamlessly parse incoming
 * JSON request bodies into Java objects, and render outgoing Java objects as JSON responses.
 *
 * <p>By providing a single interface that encompasses both directions of data binding, JSON
 * libraries can be easily registered into the Jooby environment to handle all JSON-related content
 * negotiation.
 *
 * <p><strong>Important Note:</strong> Jooby core itself <em>does not</em> implement these
 * interfaces. These contracts act as a bridge and are designed to be implemented exclusively by
 * dedicated JSON modules (such as {@code jooby-jackson}, {@code jooby-gson}, or {@code
 * jooby-avaje-json}), etc.
 *
 * @see JsonEncoder
 * @see JsonDecoder
 * @since 4.5.0
 * @author edgar
 */
public interface JsonCodec extends JsonEncoder, JsonDecoder {}
