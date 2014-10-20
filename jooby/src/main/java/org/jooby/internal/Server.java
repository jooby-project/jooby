package org.jooby.internal;

import com.google.common.annotations.Beta;

/**
 * A HTTP web server.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface Server {

  /**
   * Start the web server.
   *
   * @throws Exception If server fail to start.
   */
  void start() throws Exception;

  /**
   * Stop the web server.
   *
   * @throws Exception If server fail to stop.
   */
  void stop() throws Exception;

}
