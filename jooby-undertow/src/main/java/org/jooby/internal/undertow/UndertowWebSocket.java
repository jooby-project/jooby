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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.undertow;

import static java.util.Objects.requireNonNull;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;
import org.jooby.WebSocket.ErrCallback;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.Pooled;

import com.typesafe.config.Config;

public class UndertowWebSocket extends AbstractReceiveListener implements NativeWebSocket {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(WebSocket.class);

  private WebSocketChannel channel;

  private Consumer<String> onTextCallback;

  private Consumer<ByteBuffer> onBinaryCallback;

  private BiConsumer<Integer, Optional<String>> onCloseCallback;

  private Consumer<Throwable> onErrorCallback;

  private long maxBinaryBufferSize;

  private long maxTextBufferSize;

  private Runnable onConnectCallback;

  private long idleTimeout;

  public UndertowWebSocket(final Config config) {
    idleTimeout = config.getDuration("undertow.ws.IdleTimeout", TimeUnit.MILLISECONDS);
    maxBinaryBufferSize = config.getBytes("undertow.ws.MaxBinaryBufferSize");
    maxTextBufferSize = config.getBytes("undertow.ws.MaxTextBufferSize");
  }

  public void connect(final WebSocketChannel channel) {
    this.channel = channel;

    this.onConnectCallback.run();

    this.channel.setIdleTimeout(idleTimeout);
    this.channel.getReceiveSetter().set(this);
    this.channel.resumeReceives();
  }

  @Override
  protected long getMaxBinaryBufferSize() {
    return maxBinaryBufferSize;
  }

  @Override
  protected long getMaxTextBufferSize() {
    return maxTextBufferSize;
  }

  @Override
  public void onConnect(final Runnable callback) {
    this.onConnectCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  protected void onFullTextMessage(final WebSocketChannel channel, final BufferedTextMessage message)
      throws IOException {
    onTextCallback.accept(message.getData());
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
  protected void onFullBinaryMessage(final WebSocketChannel channel,
      final BufferedBinaryMessage message)
      throws IOException {
    Pooled<ByteBuffer[]> data = message.getData();
    try {
      this.onBinaryCallback.accept(WebSockets.mergeBuffers(data.getResource()));
    } finally {
      data.free();
    }
  }

  @Override
  protected void onCloseMessage(final CloseMessage cm, final WebSocketChannel channel) {
    onCloseCallback.accept(cm.getCode(), Optional.ofNullable(cm.getReason()));
  }

  @Override
  protected void onFullCloseMessage(final WebSocketChannel channel, final BufferedBinaryMessage message)
      throws IOException {
    // TODO Auto-generated method stub
    super.onFullCloseMessage(channel, message);
  }

  @Override
  protected void onError(final WebSocketChannel channel, final Throwable cause) {
    onErrorCallback.accept(cause);
    super.onError(channel, cause);
  }

  @Override
  public void close(final int status, final String reason) {
    WebSockets.sendClose(status, reason, channel, new WebSocketCallback<Void>() {

      @Override
      public void onError(final WebSocketChannel channel, final Void context,
          final Throwable throwable) {
        log.error("closing web socket resulted in exception: " + status, throwable);
        IoUtils.safeClose(channel);
      }

      @Override
      public void complete(final WebSocketChannel channel, final Void context) {
        IoUtils.safeClose(channel);
      }
    });

  }

  @Override
  public void resume() {
    channel.resumeReceives();
  }

  @Override
  public void pause() {
    channel.suspendReceives();
  }

  @Override
  public void terminate() throws IOException {
    this.onCloseCallback.accept(1006, Optional.of("Harsh disconnect"));
    IoUtils.safeClose(channel);
  }

  @Override
  public void send(final ByteBuffer data, final SuccessCallback success, final ErrCallback err) {
    WebSockets.sendBinary(data, channel, callback(log, success, err));
  }

  @Override
  public void send(final String data, final SuccessCallback success, final ErrCallback err) {
    WebSockets.sendText(data, channel, callback(log, success, err));

  }

  private static WebSocketCallback<Void> callback(final Logger log, final SuccessCallback success,
      final ErrCallback err) {
    return new WebSocketCallback<Void>() {
      @Override
      public void complete(final WebSocketChannel channel, final Void context) {
        try {
          success.invoke();
        } catch (Exception ex) {
          log.debug("Error while invoking write success callback", ex);
        }
      }

      @Override
      public void onError(final WebSocketChannel channel, final Void context, final Throwable cause) {
        err.invoke(cause);
      }
    };
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }
}
