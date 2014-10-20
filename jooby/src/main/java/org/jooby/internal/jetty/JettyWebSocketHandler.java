package org.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Variant;
import org.jooby.WebSocket;
import org.jooby.Route.Err;
import org.jooby.internal.VariantImpl;
import org.jooby.internal.WebSocketBinaryMessage;
import org.jooby.internal.WebSocketImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.typesafe.config.Config;

public class JettyWebSocketHandler implements WebSocketListener {

  private Injector injector;

  private WebSocketImpl socket;

  private boolean closeOnErr;

  private Logger log = LoggerFactory.getLogger(WebSocket.class);

  public JettyWebSocketHandler(final Injector injector, final Config config,
      final WebSocketImpl socket) {
    this.injector = injector;
    this.socket = socket;
    this.closeOnErr = config.getBoolean("jetty.ws.closeOnError");
  }

  @Override
  public void onWebSocketBinary(final byte[] array, final int offset, final int len) {
    try {
      // for Web Socket, charset is always UTF-8
      Variant variant = new WebSocketBinaryMessage(ByteBuffer.wrap(array, offset, len));
      socket.fireMessage(variant);
    } catch (Exception ex) {
      onWebSocketError(ex);
    }
  }

  @Override
  public void onWebSocketText(final String value) {
    try {
      // for Web Socket, charset is always UTF-8
      Variant variant = new VariantImpl(injector, "message", ImmutableList.of(value),
          socket.consumes(), Charsets.UTF_8);
      socket.fireMessage(variant);
    } catch (Exception ex) {
      onWebSocketError(ex);
    }
  }

  @Override
  public void onWebSocketClose(final int statusCode, final String reason) {
    try {
      socket.fireClose(new WebSocket.CloseStatus(statusCode, reason));
    } catch (Exception ex) {
      onWebSocketError(ex);
    }
  }

  @Override
  public void onWebSocketConnect(final Session session) {
    try {
      socket.connect(injector, session);
    } catch (Exception ex) {
      onWebSocketError(ex);
    }
  }

  @Override
  public void onWebSocketError(final Throwable cause) {
    if (cause instanceof Exception) {
      log.debug("execution resulted in exception", cause);
      try {
        socket.fireErr((Exception) cause);
      } catch (Exception ex) {
        log.error("execution of error callback resulted in exception", ex);
      }
    }
    // default close status
    if (closeOnErr && socket.isOpen()) {
      WebSocket.CloseStatus closeStatus = WebSocket.SERVER_ERROR;
      if (cause instanceof IllegalArgumentException) {
        closeStatus = WebSocket.BAD_DATA;
      } else if (cause instanceof NoSuchElementException) {
        closeStatus = WebSocket.BAD_DATA;
      } else if (cause instanceof Route.Err) {
        Route.Err err = (Err) cause;
        if (err.status() == Response.Status.BAD_REQUEST) {
          closeStatus = WebSocket.BAD_DATA;
        }
      }
      socket.close(closeStatus.code(), closeStatus.reason() + " " + cause.getMessage());
    } else {
      log.error("execution resulted in serious error", cause);
    }
  }

}
