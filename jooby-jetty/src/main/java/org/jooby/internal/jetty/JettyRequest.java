package org.jooby.internal.jetty;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;
import org.jooby.servlet.ServletServletRequest;

public class JettyRequest extends ServletServletRequest {

  public JettyRequest(final HttpServletRequest req, final String tmpdir, final boolean multipart)
      throws IOException {
    super(req, tmpdir, multipart);
  }

  @Override
  public void push(final String method, final String path, final Map<String, String> headers) {
    ((Request) servletRequest()).getPushBuilder().method(method).path(path).push();
  }
}
