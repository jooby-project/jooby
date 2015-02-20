package org.jooby.internal.jetty;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.MediaType;
import org.jooby.servlet.ServletServletRequest;
import org.jooby.servlet.ServletServletResponse;
import org.jooby.servlet.ServletUpgrade;
import org.jooby.spi.Dispatcher;
import org.jooby.spi.NativeWebSocket;

public class JettyHandler extends AbstractHandler {

  private Dispatcher dispatcher;

  private WebSocketServerFactory webSocketServerFactory;

  private String tmpdir;

  private MultipartConfigElement multiPartConfig;

  public JettyHandler(final Dispatcher dispatcher,
      final WebSocketServerFactory webSocketServerFactory, final String tmpdir) {
    this.dispatcher = dispatcher;
    this.webSocketServerFactory = webSocketServerFactory;
    this.tmpdir = tmpdir;
    this.multiPartConfig = new MultipartConfigElement(tmpdir);
  }

  @Override
  public void handle(final String target, final Request baseRequest,
      final HttpServletRequest request, final HttpServletResponse response) throws IOException,
      ServletException {
    try {

      String type = baseRequest.getContentType();
      boolean multipart = false;
      if (type != null && type.toLowerCase().startsWith(MediaType.multipart.name())) {
        baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multiPartConfig);
        multipart = true;
      }

      baseRequest.setHandled(true);

      dispatcher.handle(
          new ServletServletRequest(request, tmpdir, multipart)
              .with(new ServletUpgrade() {

                @SuppressWarnings("unchecked")
                @Override
                public <T> T upgrade(final Class<T> type) throws Exception {
                  if (type == NativeWebSocket.class) {
                    if (webSocketServerFactory.isUpgradeRequest(request, response)) {
                      if (webSocketServerFactory.acceptWebSocket(request, response)) {
                        String key = JettyWebSocket.class.getName();
                        NativeWebSocket ws = (NativeWebSocket) request.getAttribute(key);
                        request.removeAttribute(key);
                        checkState(ws != null, "Upgrade didn't success");
                        return (T) ws;
                      }
                    }
                  }
                  throw new UnsupportedOperationException("Not Supported: " + type);
                }
              }),
          new ServletServletResponse(request, response)
          );
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
  }

}
