package org.jooby.internal.netty;

import static java.util.Objects.requireNonNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.Attribute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;
import org.jooby.WebSocket.ErrCallback;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;

public class NettyWebSocket implements NativeWebSocket {

  private ChannelHandlerContext ctx;

  private Consumer<NettyWebSocket> handshake;

  private Runnable onConnectCallback;

  private WebSocketServerHandshaker handshaker;

  private Consumer<String> onTextCallback;

  private Consumer<ByteBuffer> onBinaryCallback;

  private BiConsumer<Integer, Optional<String>> onCloseCallback;

  private Consumer<Throwable> onErrorCallback;

  public NettyWebSocket(final ChannelHandlerContext ctx,
      final WebSocketServerHandshaker handshaker, final Consumer<NettyWebSocket> handshake) {
    this.ctx = ctx;
    this.handshaker = handshaker;
    this.handshake = handshake;
  }

  @Override
  public void close(final int status, final String reason) {
    handshaker.close(ctx.channel(), new CloseWebSocketFrame(status, reason));
    Attribute<NettyWebSocket> ws = ctx.attr(NettyHandler.WS);
    if (ws != null) {
      ws.remove();
    }
  }

  @Override
  public void resume() {
    if (!ctx.channel().config().isAutoRead()) {
      ctx.channel().config().setAutoRead(true);
    }
  }

  @Override
  public void onConnect(final Runnable callback) {
    this.onConnectCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void onTextMessage(final Consumer<String> callback) {
    this.onTextCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void onBinaryMessage(final Consumer<ByteBuffer> callback) {
    this.onBinaryCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void onCloseMessage(final BiConsumer<Integer, Optional<String>> callback) {
    this.onCloseCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void onErrorMessage(final Consumer<Throwable> callback) {
    this.onErrorCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void pause() {
    if (ctx.channel().config().isAutoRead()) {
      ctx.channel().config().setAutoRead(false);
    }
  }

  @Override
  public void terminate() throws IOException {
    this.onCloseCallback.accept(1006, Optional.of("Harsh disconnect"));
    ctx.disconnect();
  }

  @Override
  public void send(final ByteBuffer data, final SuccessCallback success, final ErrCallback err) {
    ByteBuf buffer = Unpooled.copiedBuffer(data);
    ctx.channel().write(new BinaryWebSocketFrame(buffer)).addListener(future -> {
      if (future.isSuccess()) {
        success.invoke();
      } else {
        err.invoke(future.cause());
      }
    });
  }

  @Override
  public void send(final String data, final SuccessCallback success, final ErrCallback err) {
    ctx.channel().write(new TextWebSocketFrame(data)).addListener(future -> {
      if (future.isSuccess()) {
        success.invoke();
      } else {
        err.invoke(future.cause());
      }
    });
  }

  @Override
  public boolean isOpen() {
    return ctx.channel().isOpen();
  }

  public void connect() {
    onConnectCallback.run();
  }

  public void hankshake() {
    handshake.accept(this);
  }

  public void handle(final Object msg) {
    if (msg instanceof TextWebSocketFrame) {
      onTextCallback.accept(((TextWebSocketFrame) msg).text());
    } else if (msg instanceof BinaryWebSocketFrame) {
      onBinaryCallback.accept(((BinaryWebSocketFrame) msg).content().nioBuffer());
    } else if (msg instanceof CloseWebSocketFrame) {
      CloseWebSocketFrame closeFrame = ((CloseWebSocketFrame) msg).retain();
      int statusCode = closeFrame.statusCode();
      onCloseCallback.accept(statusCode == -1 ? WebSocket.NORMAL.code() : statusCode,
          Optional.ofNullable(closeFrame.reasonText()));
      handshaker.close(ctx.channel(), closeFrame);
    } else if (msg instanceof Throwable) {
      onErrorCallback.accept((Throwable) msg);
    }
  }

}
