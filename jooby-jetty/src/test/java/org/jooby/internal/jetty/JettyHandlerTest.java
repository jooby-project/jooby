package org.jooby.internal.jetty;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.servlet.ServletServletRequest;
import org.jooby.servlet.ServletServletResponse;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.NativeWebSocket;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

public class JettyHandlerTest {

  private Block wsStopTimeout = unit -> {
    WebSocketServerFactory ws = unit.get(WebSocketServerFactory.class);
    ws.setStopTimeout(30000L);
  };

  @Test
  public void handleShouldSetMultipartConfig() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("Multipart/Form-Data");

          request.setAttribute(eq(Request.__MULTIPART_CONFIG_ELEMENT),
              isA(MultipartConfigElement.class));
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(isA(ServletServletRequest.class), isA(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }

  @Test
  public void handleShouldIgnoreMultipartConfig() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(isA(ServletServletRequest.class), isA(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }

  @Test
  public void handleWsUpgrade() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class, NativeWebSocket.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          NativeWebSocket ws = unit.get(NativeWebSocket.class);

          WebSocketServerFactory factory = unit.get(WebSocketServerFactory.class);

          expect(factory.isUpgradeRequest(req, rsp)).andReturn(true);

          expect(factory.acceptWebSocket(req, rsp)).andReturn(true);

          expect(req.getAttribute(JettyWebSocket.class.getName())).andReturn(ws);
          req.removeAttribute(JettyWebSocket.class.getName());
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(unit.capture(ServletServletRequest.class),
              unit.capture(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        }, unit -> {
          ServletServletRequest req = unit.captured(ServletServletRequest.class).get(0);
          req.upgrade(NativeWebSocket.class);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void handleThrowUnsupportedOperationExceptionWhenWsIsMissing() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class, NativeWebSocket.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);

          WebSocketServerFactory factory = unit.get(WebSocketServerFactory.class);

          expect(factory.isUpgradeRequest(req, rsp)).andReturn(true);

          expect(factory.acceptWebSocket(req, rsp)).andReturn(true);

          expect(req.getAttribute(JettyWebSocket.class.getName())).andReturn(null);
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(unit.capture(ServletServletRequest.class),
              unit.capture(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        }, unit -> {
          ServletServletRequest req = unit.captured(ServletServletRequest.class).get(0);
          req.upgrade(NativeWebSocket.class);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void handleThrowUnsupportedOperationExceptionOnNoWebSocketRequest() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class, NativeWebSocket.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);

          WebSocketServerFactory factory = unit.get(WebSocketServerFactory.class);

          expect(factory.isUpgradeRequest(req, rsp)).andReturn(false);
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(unit.capture(ServletServletRequest.class),
              unit.capture(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        }, unit -> {
          ServletServletRequest req = unit.captured(ServletServletRequest.class).get(0);
          req.upgrade(NativeWebSocket.class);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void handleThrowUnsupportedOperationExceptionOnHankshakeRejection() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class, NativeWebSocket.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpServletRequest req = unit.get(HttpServletRequest.class);
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);

          WebSocketServerFactory factory = unit.get(WebSocketServerFactory.class);

          expect(factory.isUpgradeRequest(req, rsp)).andReturn(true);

          expect(factory.acceptWebSocket(req, rsp)).andReturn(false);
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(unit.capture(ServletServletRequest.class),
              unit.capture(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        }, unit -> {
          ServletServletRequest req = unit.captured(ServletServletRequest.class).get(0);
          req.upgrade(NativeWebSocket.class);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void handleThrowUnsupportedOperationExceptionOnWrongType() throws Exception {
    new MockUnit(Request.class, HttpHandler.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class, NativeWebSocket.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          HttpHandler dispatcher = unit.get(HttpHandler.class);
          dispatcher.handle(unit.capture(ServletServletRequest.class),
              unit.capture(ServletServletResponse.class));
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(unit.get(HttpHandler.class), unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        }, unit -> {
          ServletServletRequest req = unit.captured(ServletServletRequest.class).get(0);
          req.upgrade(JettyHandlerTest.class);
        });
  }

  @Test(expected = ServletException.class)
  public void handleShouldReThrowServletException() throws Exception {
    HttpHandler dispatcher = (request, response) -> {
      throw new ServletException("intentional err");
    };
    new MockUnit(Request.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(false);
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(dispatcher, unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }

  @Test(expected = IOException.class)
  public void handleShouldReThrowIOException() throws Exception {
    HttpHandler dispatcher = (request, response) -> {
      throw new IOException("intentional err");
    };
    new MockUnit(Request.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(false);
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(dispatcher, unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void handleShouldReThrowIllegalArgumentException() throws Exception {
    HttpHandler dispatcher = (request, response) -> {
      throw new IllegalArgumentException("intentional err");
    };
    new MockUnit(Request.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(false);
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(dispatcher, unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }

  @Test(expected = IllegalStateException.class)
  public void handleShouldReThrowIllegalStateException() throws Exception {
    HttpHandler dispatcher = (request, response) -> {
      throw new Exception("intentional err");
    };
    new MockUnit(Request.class, WebSocketServerFactory.class,
        HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(true);

          expect(request.getContentType()).andReturn("application/json");
        })
        .expect(unit -> {
          HttpServletRequest request = unit.get(HttpServletRequest.class);

          expect(request.getRequestURI()).andReturn("/");
        })
        .expect(unit -> {
          Request request = unit.get(Request.class);

          request.setHandled(false);
        })
        .expect(wsStopTimeout)
        .run(unit -> {
          new JettyHandler(dispatcher, unit.get(WebSocketServerFactory.class),
              "target")
              .handle("/", unit.get(Request.class),
                  unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class));
        });
  }
}
