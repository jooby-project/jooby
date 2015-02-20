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

public interface NativeWebSocket {

  void close(int status, String reason);

  void resume();

  void onConnect(Runnable callback);

  void onTextMessage(Consumer<String> callback);

  void onBinaryMessage(Consumer<ByteBuffer> callback);

  void onCloseMessage(BiConsumer<Integer, Optional<String>> callback);

  void onErrorMessage(Consumer<Throwable> callback);

  void pause();

  void terminate() throws IOException;

  void send(ByteBuffer data, WebSocket.SuccessCallback success, WebSocket.ErrCallback err);

  void send(String data, WebSocket.SuccessCallback success, WebSocket.ErrCallback err);

  boolean isOpen();

}
