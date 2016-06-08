/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;
import org.jooby.WebSocket.ErrCallback;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyWebSocket implements NativeWebSocket {

  public static final AttributeKey<NettyWebSocket> KEY =
      AttributeKey.newInstance(NettyWebSocket.class.getName());

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private ChannelHandlerContext ctx;

  private Consumer<NettyWebSocket> handshake;

  private Runnable onConnectCallback;

  private WebSocketServerHandshaker handshaker;

  private Consumer<String> onTextCallback;

  private Consumer<ByteBuffer> onBinaryCallback;

  private BiConsumer<Integer, Optional<String>> onCloseCallback;

  private Consumer<Throwable> onErrorCallback;

  private final CountDownLatch ready = new CountDownLatch(1);

  public NettyWebSocket(final ChannelHandlerContext ctx,
      final WebSocketServerHandshaker handshaker, final Consumer<NettyWebSocket> handshake) {
    this.ctx = ctx;
    this.handshaker = handshaker;
    this.handshake = handshake;
  }

  @Override
  public void close(final int status, final String reason) {
    handshaker.close(ctx.channel(), new CloseWebSocketFrame(status, reason))
        .addListener(FIRE_EXCEPTION_ON_FAILURE);
    Attribute<NettyWebSocket> ws = ctx.channel().attr(KEY);
    if (ws != null) {
      ws.remove();
    }
  }

  @Override
  public void resume() {
    ChannelConfig config = ctx.channel().config();
    if (!config.isAutoRead()) {
      config.setAutoRead(true);
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
    ChannelConfig config = ctx.channel().config();
    if (config.isAutoRead()) {
      config.setAutoRead(false);
    }
  }

  @Override
  public void terminate() throws IOException {
    this.onCloseCallback.accept(1006, Optional.of("Harsh disconnect"));
    ctx.disconnect().addListener(FIRE_EXCEPTION_ON_FAILURE);
  }

  @Override
  public void sendBytes(final ByteBuffer data, final SuccessCallback success, final ErrCallback err) {
    sendBytes(Unpooled.wrappedBuffer(data), success, err);
  }

  @Override
  public void sendBytes(final byte[] data, final SuccessCallback success, final ErrCallback err) {
    sendBytes(Unpooled.wrappedBuffer(data), success, err);
  }

  @Override
  public void sendText(final String data, final SuccessCallback success, final ErrCallback err) {
    ctx.channel().writeAndFlush(new TextWebSocketFrame(data))
        .addListener(listener(success, err));
  }

  @Override
  public void sendText(final ByteBuffer data, final SuccessCallback success, final ErrCallback err) {
    ByteBuf buffer = Unpooled.wrappedBuffer(data);
    ctx.channel().writeAndFlush(new TextWebSocketFrame(buffer))
        .addListener(listener(success, err));
  }

  @Override
  public void sendText(final byte[] data, final SuccessCallback success, final ErrCallback err) {
    ByteBuf buffer = Unpooled.wrappedBuffer(data);
    ctx.channel().writeAndFlush(new TextWebSocketFrame(buffer))
        .addListener(listener(success, err));
  }

  @Override
  public boolean isOpen() {
    return ctx.channel().isOpen();
  }

  public void connect() {
    onConnectCallback.run();
    ready.countDown();
  }

  public void hankshake() {
    handshake.accept(this);
  }

  public void handle(final Object msg) {
    ready();
    if (msg instanceof TextWebSocketFrame) {
      onTextCallback.accept(((TextWebSocketFrame) msg).text());
    } else if (msg instanceof BinaryWebSocketFrame) {
      onBinaryCallback.accept(((BinaryWebSocketFrame) msg).content().nioBuffer());
    } else if (msg instanceof CloseWebSocketFrame) {
      CloseWebSocketFrame closeFrame = ((CloseWebSocketFrame) msg).retain();
      int statusCode = closeFrame.statusCode();
      onCloseCallback.accept(statusCode == -1 ? WebSocket.NORMAL.code() : statusCode,
          Optional.ofNullable(closeFrame.reasonText()));
      handshaker.close(ctx.channel(), closeFrame).addListener(FIRE_EXCEPTION_ON_FAILURE);
    } else if (msg instanceof Throwable) {
      onErrorCallback.accept((Throwable) msg);
    }
  }

  /**
   * Make sure hankshake/connect is set.
   */
  private void ready() {
    try {
      ready.await();
    } catch (InterruptedException ex) {
      log.error("Connect call was inturrupted", ex);
      Thread.currentThread().interrupt();
    }
  }

  private void sendBytes(final ByteBuf buffer, final SuccessCallback success, final ErrCallback err) {
    ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer))
        .addListener(listener(success, err));
  }

  private GenericFutureListener<? extends Future<? super Void>> listener(
      final SuccessCallback success, final ErrCallback err) {
    return f -> {
      if (f.isSuccess()) {
        success.invoke();
      } else {
        err.invoke(f.cause());
      }
    };
  }

}
