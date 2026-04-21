/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.ws;

import java.util.EnumMap;
import java.util.Set;

final class WsParamTypes {

  static final String RAW_WEBSOCKET = "io.jooby.WebSocket";
  static final String RAW_CONTEXT = "io.jooby.Context";
  static final String RAW_MESSAGE = "io.jooby.WebSocketMessage";
  static final String RAW_CLOSE_STATUS = "io.jooby.WebSocketCloseStatus";
  static final String RAW_THROWABLE = "java.lang.Throwable";

  private static final EnumMap<WsLifecycle, Set<String>> ALLOWED_TYPES =
      new EnumMap<>(WsLifecycle.class);

  static {
    ALLOWED_TYPES.put(WsLifecycle.CONNECT, Set.of(RAW_WEBSOCKET, RAW_CONTEXT));
    ALLOWED_TYPES.put(
        WsLifecycle.MESSAGE, Set.of(RAW_WEBSOCKET, RAW_CONTEXT, RAW_MESSAGE));
    ALLOWED_TYPES.put(
        WsLifecycle.CLOSE, Set.of(RAW_WEBSOCKET, RAW_CONTEXT, RAW_CLOSE_STATUS));
    ALLOWED_TYPES.put(
        WsLifecycle.ERROR, Set.of(RAW_WEBSOCKET, RAW_CONTEXT, RAW_THROWABLE));
  }

  static Set<String> getAllowedTypes(WsLifecycle lifecycle) {
    return ALLOWED_TYPES.get(lifecycle);
  }

  static String generateArgumentName(String rawType) {
    return switch (rawType) {
      case RAW_WEBSOCKET -> "ws";
      case RAW_CONTEXT -> "ctx";
      case RAW_MESSAGE -> "message";
      case RAW_CLOSE_STATUS -> "status";
      case RAW_THROWABLE -> "cause";
      default -> null;
    };
  }
}
