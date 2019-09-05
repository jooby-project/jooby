package io.jooby.internal.netty;

import io.jooby.StatusCode;
import io.jooby.WebSocket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class NettyWebSocket implements WebSocket {
  private final NettyContext ctx;
  private final boolean dispatch;
  private ByteBuf buffer;
  private OnConnect connect;
  private OnMessage message;

  public NettyWebSocket(NettyContext ctx) {
    this.ctx = ctx;
    dispatch = !ctx.isInIoThread();
  }

   public void send(String text) {
    ctx.ctx.channel().writeAndFlush(new TextWebSocketFrame(text));
  }

  @Override public void onConnect(OnConnect listener) {
    connect = listener;
  }

  @Override public void onMessage(OnMessage listener) {
    message = listener;
  }

  @Override public void onError(WebSocket ctx, Throwable cause, StatusCode statusCode) {

  }

  @Override public void onClose(WebSocket ctx, StatusCode reason) {

  }

  public void handleFrame(WebSocketFrame frame) {
    if (frame.isFinalFragment()) {
      ByteBuf content;
      if (buffer != null) {
        buffer.writeBytes(frame.content());
        content = buffer;
        buffer = null;
      } else {
        content = frame.content();
      }
      if (dispatch) {
//        ctx.getRouter().getWorker()
//            .execute(() -> this.message.onMessage(ctx);
      } else {
//        this.message.onMessage(ctx, content.toString(StandardCharsets.UTF_8));
      }
    } else {
      buffer = Unpooled.copiedBuffer(frame.content());
    }
  }

  public void fireConnect(NettyContext ctx) {
    if (connect != null) {
      if (dispatch) {
        ctx.getRouter().getWorker().execute(() -> connect.onConnect(this));
      } else {
        connect.onConnect(this);
      }
    }
  }
}
