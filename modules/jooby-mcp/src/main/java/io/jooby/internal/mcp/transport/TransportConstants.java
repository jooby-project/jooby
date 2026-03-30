/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import io.jooby.MediaType;

/**
 * @author kliushnichenko
 */
class TransportConstants {

  public static final MediaType TEXT_EVENT_STREAM = MediaType.valueOf("text/event-stream");
  public static final String MESSAGE_EVENT_TYPE = "message";
  public static final String SSE_ERROR_EVENT = "Error";
}
