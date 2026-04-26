/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import io.opentelemetry.context.Context;

/**
 * Strategy interface for retrieving an active OpenTelemetry {@link Context} from a {@link
 * io.jooby.Context}.
 *
 * <p>When a request is intercepted by the OpenTelemetry HTTP tracing module, the active distributed
 * tracing context is captured and attached to the Jooby request lifecycle. This interface provides
 * a decoupled mechanism to extract that context later in the pipeline.
 *
 * <p>This is particularly critical when execution crosses asynchronous boundaries or worker threads
 * (such as in JSON-RPC or Model Context Protocol execution), where {@link Context#current()} would
 * otherwise return empty. By retrieving the context via this interface, extensions can explicitly
 * set the parent context for newly spawned child spans.
 *
 * @author edgar
 * @since 4.3.1
 */
public interface OtelContextExtractor {
  /**
   * Retrieves the OpenTelemetry context associated with the given HTTP request.
   *
   * @param ctx The current Jooby HTTP context.
   * @return The active OpenTelemetry {@link Context}, or {@code null} if no tracing context was
   *     initialized or attached to this request.
   */
  Context extract(io.jooby.Context ctx);
}
