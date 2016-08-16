package org.jooby.internal.jetty;

import java.util.Map;

import org.eclipse.jetty.server.PushBuilder;
import org.eclipse.jetty.server.Request;
import org.jooby.spi.NativePushPromise;

public class JettyPush implements NativePushPromise {

  private Request req;

  public JettyPush(final Request req) {
    this.req = req;
  }

  @Override
  public void push(final String method, final String path, final Map<String, String> headers) {
    PushBuilder pb = req.getPushBuilder()
        .path(path)
        .method(method);
    headers.forEach(pb::addHeader);
    pb.push();
  }

}
