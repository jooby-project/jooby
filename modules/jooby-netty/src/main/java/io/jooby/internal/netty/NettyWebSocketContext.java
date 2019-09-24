package io.jooby.internal.netty;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketContext;
import io.jooby.WebSocketMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class NettyWebSocketContext implements WebSocketContext, WebSocket {
  private final NettyContext netty;
  private final boolean dispatch;
  private ByteBuf buffer;
  private WebSocket.OnConnect connectCallback;
  private WebSocket.OnMessage messageCallback;
  private OnClose onCloseCallback;
  private OnError onErrorCallback;

  public NettyWebSocketContext(NettyContext ctx) {
    this.netty = ctx;
    dispatch = !ctx.isInIoThread();
  }

  public WebSocket send(String text) {
    return send(new TextWebSocketFrame(text));
  }

  public WebSocket send(byte[] bytes) {
    return send(new TextWebSocketFrame(Unpooled.wrappedBuffer(bytes)));
  }

  private WebSocket send(TextWebSocketFrame frame) {
    if (isOpen()) {
      netty.ctx.channel().writeAndFlush(frame);
    } else {
      handleError(new IllegalStateException("Attempt to send a message on closed web socket"));
    }
    return this;
  }

  @Override public WebSocket render(Object message) {
    Context.websocket(netty, this).render(message);
    return this;
  }

  @Override public Context getContext() {
    return Context.readOnly(netty);
  }

  public boolean isOpen() {
    return netty.ctx.channel().isOpen();
  }

  @Override public void onConnect(WebSocket.OnConnect callback) {
    connectCallback = callback;
  }

  @Override public void onMessage(WebSocket.OnMessage callback) {
    messageCallback = callback;
  }

  @Override public void onClose(WebSocket.OnClose callback) {
    onCloseCallback = callback;
  }

  @Override public void onError(OnError callback) {
    onErrorCallback = callback;
  }

  void handleFrame(WebSocketFrame frame) {
    try {
      if (frame instanceof TextWebSocketFrame || frame instanceof BinaryWebSocketFrame
          || frame instanceof ContinuationWebSocketFrame) {
        handleMessage(frame);
      } else if (frame instanceof CloseWebSocketFrame) {
        handleClose(toWebSocketCloseStatus((CloseWebSocketFrame) frame));
      }
    } catch (Throwable x) {
      handleError(x);
    }
  }

  private void handleMessage(WebSocketFrame frame) {
    try {
      if (frame.isFinalFragment()) {
        ByteBuf content;
        if (buffer != null) {
          buffer.writeBytes(frame.content());
          content = buffer;
          buffer = null;
        } else {
          content = frame.content();
        }
        WebSocketMessage message = WebSocketMessage.create(getContext(), array(content));

        fireCallback(webSocketTask(() -> messageCallback.onMessage(this, message)));
      } else {
        buffer = Unpooled.copiedBuffer(frame.content());
      }
    } finally {
      frame.release();
    }
  }

  private void handleClose(WebSocketCloseStatus closeStatus) {
    if (isOpen()) {
      if (onCloseCallback != null) {
        Runnable task = webSocketTask(() -> onCloseCallback.onClose(this, closeStatus));
        Runnable closeCallback = () -> {
          try {
            task.run();
          } finally {
            netty.ctx.channel()
                .writeAndFlush(
                    new CloseWebSocketFrame(closeStatus.getCode(), closeStatus.getReason()))
                .addListener(ChannelFutureListener.CLOSE);
          }
        };

        fireCallback(closeCallback);
      }
    }
  }

  private void handleError(Throwable x) {
    // should close?
    if (Server.connectionLost(x) || SneakyThrows.isFatal(x)) {
      handleClose(WebSocketCloseStatus.SERVER_ERROR);
    }

    if (onErrorCallback == null) {
      netty.getRouter().getLog().error("WS {} resulted in exception", netty.pathString(), x);
    } else {
      onErrorCallback.onError(this, x);
    }

    if (SneakyThrows.isFatal(x)) {
      throw SneakyThrows.propagate(x);
    }
  }

  void fireConnect() {
    if (connectCallback != null) {
      fireCallback(webSocketTask(() -> connectCallback.onConnect(this)));
    }
  }

  private Runnable webSocketTask(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Throwable x) {
        handleError(x);
      }
    };
  }

  private void fireCallback(Runnable task) {
    if (dispatch) {
      Router router = netty.getRouter();
      router.getWorker().execute(task);
    } else {
      task.run();
    }
  }

  private static byte[] array(ByteBuf buffer) {
    if (buffer.hasArray()) {
      return buffer.array();
    } else {
      byte[] bytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, bytes);
      return bytes;
    }
  }

  private static WebSocketCloseStatus toWebSocketCloseStatus(CloseWebSocketFrame frame) {
    try {
      return WebSocketCloseStatus
          .valueOf(frame.statusCode())
          .orElseGet(() -> new WebSocketCloseStatus(frame.statusCode(), frame.reasonText()));
    } finally {
      frame.release();
    }
  }
}
