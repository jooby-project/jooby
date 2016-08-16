package org.jooby.spi;

import java.util.Map;

/**
 * HTTP/2 push promise.
 *
 * @author edgar
 */
public interface NativePushPromise {

  /**
   * Send a push promise to client and start/enqueue a the response.
   *
   * @param method HTTP method.
   * @param path Resource path.
   * @param headers Resource headers.
   */
  void push(String method, String path, Map<String, String> headers);

}
