/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * Websocket configurer. Allow to register callbacks for websocket.
 *
 * @author edgar
 * @since 2.2.0
 */
public interface WebSocketConfigurer {

  /**
   * Register an <code>onConnect</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  WebSocketConfigurer onConnect(WebSocket.OnConnect callback);

  /**
   * Register an <code>onMessage</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  WebSocketConfigurer onMessage(WebSocket.OnMessage callback);

  /**
   * Register an <code>onError</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  WebSocketConfigurer onError(WebSocket.OnError callback);

  /**
   * Register an <code>onClose</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  WebSocketConfigurer onClose(WebSocket.OnClose callback);
}
