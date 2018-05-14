package org.jooby.undertow;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import org.jooby.internal.undertow.UndertowRequest;
import org.jooby.internal.undertow.UndertowResponse;
import org.jooby.spi.NativeWebSocket;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowResponse.class, HttpServerExchange.class, Handlers.class})
public class UndertowResponseTest {

  @Test
  public void newObject() throws Exception {
    new MockUnit(HttpServerExchange.class)
        .run(unit -> {
          new UndertowResponse(unit.get(HttpServerExchange.class));
        });
  }

  @Test
  public void isResponseStarted() throws Exception {
    new MockUnit(HttpServerExchange.class)
        .expect(unit -> {
          HttpServerExchange exchange = unit.get(HttpServerExchange.class);
          expect(exchange.isResponseStarted()).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true, new UndertowResponse(unit.get(HttpServerExchange.class)).committed());
        });
  }

  @Test
  public void end() throws Exception {
    new MockUnit(HttpServerExchange.class, NativeWebSocket.class,
        WebSocketProtocolHandshakeHandler.class)
        .expect(unit -> {
          HttpServerExchange exchange = unit.get(HttpServerExchange.class);
          NativeWebSocket ws = unit.get(NativeWebSocket.class);
          expect(exchange.getAttachment(UndertowRequest.SOCKET)).andReturn(ws);
          expect(exchange.removeAttachment(UndertowRequest.SOCKET)).andReturn(null);
          expect(exchange.endExchange()).andReturn(exchange);
        })
        .expect(unit -> {
          WebSocketProtocolHandshakeHandler wsphh = unit
              .get(WebSocketProtocolHandshakeHandler.class);
          wsphh.handleRequest(unit.get(HttpServerExchange.class));
          expectLastCall().andThrow(new IllegalStateException("intentional err"));

          unit.mockStatic(Handlers.class);
          expect(Handlers.websocket(isA(WebSocketConnectionCallback.class))).andReturn(wsphh);
        })
        .run(unit -> {
          new UndertowResponse(unit.get(HttpServerExchange.class)).end();
        });
  }

}
