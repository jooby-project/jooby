package org.jooby.internal.undertow;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Status;
import org.jooby.WebSocket;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.WebSocketImpl;
import org.jooby.internal.WsBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Pooled;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

public class UndertowWebSocketBridge extends AbstractReceiveListener {

  private interface BridgeCall {
    void run() throws Exception;
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(WebSocket.class);

  private Injector injector;

  private WebSocketImpl socket;

  public UndertowWebSocketBridge(final Injector injector, final WebSocketImpl socket) {
    this.injector = injector;
    this.socket = socket;
  }

  public void connect(final WebSocketChannel channel) {
    doCall(channel, () -> socket.connect(injector, channel));
  }

  @Override
  protected void onFullTextMessage(final WebSocketChannel channel,
      final BufferedTextMessage message) throws IOException {
    doCall(channel, () -> {
      Mutant variant = new MutantImpl(injector, "textMessage", ImmutableList.of(message.getData()),
          socket.consumes(), Charsets.UTF_8);
      socket.fireMessage(variant);
    });
  }

  @Override
  protected void onFullBinaryMessage(final WebSocketChannel channel,
      final BufferedBinaryMessage message)
      throws IOException {
    doCall(channel, () -> {
      Pooled<ByteBuffer[]> data = message.getData();
      try {
        Mutant variant = new WsBinaryMessage(WebSockets.mergeBuffers(data.getResource()));
        socket.fireMessage(variant);
      } finally {
        data.free();
      }
    });
  }

  @Override
  protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
    doCall(channel, () -> {
      WebSocket.CloseStatus statusCode = Optional.ofNullable(cm.getReason()).map(reason ->
          reason.length() > 0
              ? WebSocket.CloseStatus.of(cm.getCode(), reason)
              : WebSocket.CloseStatus.of(cm.getCode())
          ).orElseGet(() ->
              WebSocket.CloseStatus.of(cm.getCode())
          );
      socket.fireClose(statusCode);
    });
  }

  @Override
  protected void onError(final WebSocketChannel channel, final Throwable cause) {
    handleErr(channel, cause);
  }

  private void doCall(final WebSocketChannel channel, final BridgeCall callable) {
    try {
      callable.run();
    } catch (Throwable ex) {
      handleErr(channel, ex);
    }
  }

  private void handleErr(final WebSocketChannel channel, final Throwable cause) {
    try {
      try {
        socket.fireErr(cause);
      } catch (Throwable ex) {
        log.error("execution of WS" + socket.path() + " resulted in exception", cause);
        log.error("  error callback resulted in exception", ex);
      }
      if (channel.isOpen()) {
        WebSocket.CloseStatus closeStatus = WebSocket.SERVER_ERROR;
        if (cause instanceof IllegalArgumentException) {
          closeStatus = WebSocket.BAD_DATA;
        } else if (cause instanceof NoSuchElementException) {
          closeStatus = WebSocket.BAD_DATA;
        } else if (cause instanceof Err) {
          Err err = (Err) cause;
          if (err.statusCode() == Status.BAD_REQUEST.value()) {
            closeStatus = WebSocket.BAD_DATA;
          }
        }
        socket.close(closeStatus.code(), closeStatus.reason() + " " + cause.getMessage());
      }
    } finally {
      super.onError(channel, cause);
    }
  }

}
