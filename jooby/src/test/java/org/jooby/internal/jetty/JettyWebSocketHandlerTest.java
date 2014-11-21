package org.jooby.internal.jetty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.NoSuchElementException;

import org.eclipse.jetty.websocket.api.Session;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.Status;
import org.jooby.WebSocket;
import org.jooby.WebSocket.CloseStatus;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.WebSocketImpl;
import org.jooby.internal.WsBinaryMessage;
import org.junit.Test;

import com.google.inject.Injector;
import com.typesafe.config.Config;

public class JettyWebSocketHandlerTest {

  @Test
  public void onWebSocketBinary() throws Exception {
    byte[] bytes = "bytes".getBytes();
    int offset = 0;
    int len = bytes.length;
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);
          socket.fireMessage(isA(WsBinaryMessage.class));
        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketBinary(bytes, offset, len);
          ;
        });
  }

  @Test
  public void onWebSocketText() throws Exception {
    String value = "message";
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getTypeConverterBindings()).andReturn(Collections.emptySet());
          expect(injector.getParent()).andReturn(null);
        })
        .expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          expect(socket.consumes()).andReturn(MediaType.all);

          socket.fireMessage(unit.capture(MutantImpl.class));
        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketText(value);
          ;
        });
  }

  @Test
  public void onWebSocketClose() throws Exception {
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.fireClose(unit.capture(WebSocket.CloseStatus.class));

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketClose(1000, "Normal");
          ;
        }, unit -> {
          CloseStatus closeStatus = unit.captured(WebSocket.CloseStatus.class).iterator().next();
          assertEquals(1000, closeStatus.code());
          assertEquals("Normal", closeStatus.reason());
        });
  }

  @Test
  public void onWebSocketConnect() throws Exception {
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class, Session.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.connect(unit.get(Injector.class), unit.get(Session.class));

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketConnect(unit.get(Session.class));
          ;
        });
  }

  @Test
  public void onWebSocketError() throws Exception {
    Exception generr = new Exception("message");
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.fireErr(generr);

          expect(socket.isOpen()).andReturn(true);

          socket.close(1011, "Server error message");

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketError(generr);
          ;
        });

    IllegalArgumentException badarg = new IllegalArgumentException("message");
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.fireErr(badarg);

          expect(socket.isOpen()).andReturn(true);

          socket.close(1007, "Bad data message");

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketError(badarg);
          ;
        });

    Exception nosuchelem = new NoSuchElementException("message");
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.fireErr(nosuchelem);

          expect(socket.isOpen()).andReturn(true);

          socket.close(1007, "Bad data message");

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketError(nosuchelem);
          ;
        });

    Exception err = new Err(Status.BAD_REQUEST);
    new MockUnit(Injector.class, Config.class, WebSocketImpl.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getBoolean("jetty.ws.closeOnError")).andReturn(true);
        }).expect(unit -> {
          WebSocketImpl socket = unit.get(WebSocketImpl.class);

          socket.fireErr(err);

          expect(socket.isOpen()).andReturn(true);

          socket.close(1007, "Bad data Bad Request(400)");

        }).run(unit -> {

          new JettyWebSocketHandler(unit.get(Injector.class), unit.get(Config.class), unit
              .get(WebSocketImpl.class))
              .onWebSocketError(err);
          ;
        });
  }

}
