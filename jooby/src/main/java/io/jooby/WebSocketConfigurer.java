package io.jooby;

import javax.annotation.Nonnull;

/**
 * Websocket configurer. Allow to register callbacks for websocket.
 *
 * @author edgar
 * @since  2.2.0
 */
public interface WebSocketConfigurer {

  /**
   * Register an <code>onConnect</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  @Nonnull WebSocketConfigurer onConnect(@Nonnull WebSocket.OnConnect callback);

  /**
   * Register an <code>onMessage</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  @Nonnull WebSocketConfigurer onMessage(@Nonnull WebSocket.OnMessage callback);

  /**
   * Register an <code>onError</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  @Nonnull WebSocketConfigurer onError(@Nonnull WebSocket.OnError callback);

  /**
   * Register an <code>onClose</code> callback.
   *
   * @param callback Callback.
   * @return This configurer.
   */
  @Nonnull WebSocketConfigurer onClose(@Nonnull WebSocket.OnClose callback);
}
