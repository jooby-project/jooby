/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * HTTP/2 extension.
 *
 * @param <ServerInput> Server input.
 * @param <ServerOutput> Server output.
 */
public interface Http2Configurer<ServerInput, ServerOutput> {

  /**
   * True whenever the extension supports the current server.
   *
   * @param type Server implementation.
   * @return True whenever the extension supports the current server.
   */
  boolean support(Class type);

  /**
   * Configure server to support HTTP/2.
   *
   * @param input Server input.
   * @return Output or <code>null</code> for side-effect configurer.
   */
  ServerOutput configure(ServerInput input);
}
