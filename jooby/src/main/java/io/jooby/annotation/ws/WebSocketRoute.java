/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.ws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks class as Websocket handler.
 *
 * <p>Register the generated {@link io.jooby.Extension} with {@link io.jooby.Jooby#ws(io.jooby.Extension)}.
 *
 * <pre>{@code
 * @WebSocketRoute("/chat/{username}")
 * public class ChatWebsocket {
 *
 *   @OnMessage
 *   public String onMessage(WebSocketMessage message) { ... }
 *
 * }
 * }</pre>
 *
 *  @author kliushnichenko
 *  @since 4.4.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebSocketRoute {
  /**
   * WebSocket route patterns (Ant-style), same rules as {@link io.jooby.annotation.Path}.
   *
   * @return Patterns.
   */
  String[] value() default {};
}
