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
package org.jooby.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;

/**
 * A web socket upgrade created from {@link NativeRequest#upgrade(Class)}.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface NativeWebSocket {

  /**
   * Close the web socket.
   *
   * @param status A web socket close status.
   * @param reason A close reason.
   */
  void close(int status, String reason);

  /**
   * Resume reads.
   */
  void resume();

  /**
   * Set the onconnect callback. It will be execute once per each client.
   *
   * @param callback A callback.
   */
  void onConnect(Runnable callback);

  /**
   * Set the ontext message callback. On message arrival the callback will be executed.
   *
   * @param callback A callback.
   */
  void onTextMessage(Consumer<String> callback);

  /**
   * Set the onbinary message callback. On message arrival the callback will be executed.
   *
   * @param callback A callback.
   */
  void onBinaryMessage(Consumer<ByteBuffer> callback);

  /**
   * Set the onclose message callback. It will be executed when clients close a connection and/or
   * connection idle timeout.
   *
   * @param callback A callback.
   */
  void onCloseMessage(BiConsumer<Integer, Optional<String>> callback);

  /**
   * Set the onerror message callback. It will be executed on errors.
   *
   * @param callback A callback.
   */
  void onErrorMessage(Consumer<Throwable> callback);

  /**
   * Pause reads.
   */
  void pause();

  /**
   * Terminate immediately a connection.
   *
   * @throws IOException If termination fails.
   */
  void terminate() throws IOException;

  /**
   * Send a binary message to the client.
   *
   * @param data Message to send.
   * @param success Success callback.
   * @param err Error callback.
   */
  void sendBytes(ByteBuffer data, WebSocket.SuccessCallback success, WebSocket.OnError err);

  /**
   * Send a binary message to the client.
   *
   * @param data Message to send.
   * @param success Success callback.
   * @param err Error callback.
   */
  void sendBytes(byte[] data, WebSocket.SuccessCallback success, WebSocket.OnError err);

  /**
   * Send a text message to the client.
   *
   * @param data Message to send.
   * @param success Success callback.
   * @param err Error callback.
   */
  void sendText(String data, WebSocket.SuccessCallback success, WebSocket.OnError err);

  /**
   * Send a text message to the client.
   *
   * @param data Message to send.
   * @param success Success callback.
   * @param err Error callback.
   */
  void sendText(ByteBuffer data, WebSocket.SuccessCallback success, WebSocket.OnError err);

  /**
   * Send a text message to the client.
   *
   * @param data Message to send.
   * @param success Success callback.
   * @param err Error callback.
   */
  void sendText(byte[] data, WebSocket.SuccessCallback success, WebSocket.OnError err);

  /**
   * @return True if the websocket connection is open.
   */
  boolean isOpen();

}
