/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

/**
 * Exception that indicates the cumulative number of bytes consumed from a stream of {@link
 * DataBuffer DataBuffer}'s exceeded some pre-configured limit. This can be raised when data buffers
 * are cached and aggregated, e.g. {@link DataBufferUtils#join}. Or it could also be raised when
 * data buffers have been released but a parsed representation is being aggregated, e.g. async
 * parsing with Jackson, SSE parsing and aggregating lines per event.
 *
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
@SuppressWarnings("serial")
public class DataBufferLimitException extends IllegalStateException {

  public DataBufferLimitException(String message) {
    super(message);
  }
}
