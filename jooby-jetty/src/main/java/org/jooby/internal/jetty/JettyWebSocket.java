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
package org.jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.jooby.WebSocket;
import org.jooby.WebSocket.OnError;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javaslang.control.Try;

public class JettyWebSocket implements NativeWebSocket, WebSocketListener {

  private static final String A_CALLBACK_IS_REQUIRED = "A callback is required.";
  private static final String NO_DATA_TO_SEND = "No data to send.";
  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(WebSocket.class);

  private Session session;

  private SuspendToken suspendToken;

  private Runnable onConnectCallback;

  private Consumer<String> onTextCallback;

  private Consumer<ByteBuffer> onBinaryCallback;

  private BiConsumer<Integer, Optional<String>> onCloseCallback;

  private Consumer<Throwable> onErrorCallback;

  @Override
  public void close(final int status, final String reason) {
    session.close(status, reason);
  }

  @Override
  public void resume() {
    if (suspendToken != null) {
      suspendToken.resume();
      suspendToken = null;
    }
  }

  @Override
  public void onConnect(final Runnable callback) {
    this.onConnectCallback = requireNonNull(callback, A_CALLBACK_IS_REQUIRED);
  }

  @Override
  public void onTextMessage(final Consumer<String> callback) {
    this.onTextCallback = requireNonNull(callback, A_CALLBACK_IS_REQUIRED);
  }

  @Override
  public void onBinaryMessage(final Consumer<ByteBuffer> callback) {
    this.onBinaryCallback = requireNonNull(callback, A_CALLBACK_IS_REQUIRED);
  }

  @Override
  public void onCloseMessage(final BiConsumer<Integer, Optional<String>> callback) {
    this.onCloseCallback = requireNonNull(callback, A_CALLBACK_IS_REQUIRED);
  }

  @Override
  public void onErrorMessage(final Consumer<Throwable> callback) {
    this.onErrorCallback = requireNonNull(callback, A_CALLBACK_IS_REQUIRED);
  }

  @Override
  public void pause() {
    if (suspendToken == null) {
      suspendToken = session.suspend();
    }
  }

  @Override
  public void terminate() throws IOException {
    session.disconnect();
  }

  @Override
  public void sendBytes(final ByteBuffer data, final SuccessCallback success,
      final OnError err) {
    requireNonNull(data, NO_DATA_TO_SEND);

    RemoteEndpoint remote = session.getRemote();
    remote.sendBytes(data, callback(log, success, err));
  }

  @Override
  public void sendBytes(final byte[] data, final SuccessCallback success, final OnError err) {
    requireNonNull(data, NO_DATA_TO_SEND);
    sendBytes(ByteBuffer.wrap(data), success, err);
  }

  @Override
  public void sendText(final String data, final SuccessCallback success, final OnError err) {
    requireNonNull(data, NO_DATA_TO_SEND);

    RemoteEndpoint remote = session.getRemote();
    remote.sendString(data, callback(log, success, err));
  }

  @Override
  public void sendText(final byte[] data, final SuccessCallback success, final OnError err) {
    requireNonNull(data, NO_DATA_TO_SEND);

    RemoteEndpoint remote = session.getRemote();
    remote.sendString(new String(data, StandardCharsets.UTF_8), callback(log, success, err));
  }

  @Override
  public void sendText(final ByteBuffer data, final SuccessCallback success,
      final OnError err) {
    requireNonNull(data, NO_DATA_TO_SEND);

    RemoteEndpoint remote = session.getRemote();
    CharBuffer buffer = StandardCharsets.UTF_8.decode(data);
    // we need a TextFrame with ByteBuffer :(
    remote.sendString(buffer.toString(), callback(log, success, err));
  }

  @Override
  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
    this.onBinaryCallback.accept(ByteBuffer.wrap(payload, offset, len));
  }

  @Override
  public void onWebSocketText(final String message) {
    this.onTextCallback.accept(message);
  }

  @Override
  public void onWebSocketClose(final int statusCode, final String reason) {
    onCloseCallback.accept(statusCode, Optional.ofNullable(reason));
  }

  @Override
  public void onWebSocketConnect(final Session session) {
    this.session = session;
    this.onConnectCallback.run();
  }

  @Override
  public void onWebSocketError(final Throwable cause) {
    this.onErrorCallback.accept(cause);
  }

  static WriteCallback callback(final Logger log, final SuccessCallback success,
      final OnError err) {
    requireNonNull(success, "Success callback is required.");
    requireNonNull(err, "Error callback is required.");

    WriteCallback callback = new WriteCallback() {
      @Override
      public void writeSuccess() {
        Try.run(success::invoke)
            .onFailure(cause -> log.error("Error while invoking success callback", cause));
      }

      @Override
      public void writeFailed(final Throwable cause) {
        Try.run(() -> err.onError(cause))
            .onFailure(ex -> log.error("Error while invoking err callback", ex));
      }
    };
    return callback;
  }

}
