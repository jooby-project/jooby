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
package org.jooby.internal.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.xnio.IoUtils;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class ChunkedStream implements IoCallback, Runnable {

  private ReadableByteChannel source;

  private HttpServerExchange exchange;

  private Sender sender;

  private PooledByteBuffer pooled;

  private IoCallback callback;

  private int bufferSize;

  private int chunk;

  public void send(final ReadableByteChannel source, final HttpServerExchange exchange,
      final IoCallback callback) {
    this.source = source;
    this.exchange = exchange;
    this.callback = callback;
    this.sender = exchange.getResponseSender();
    ServerConnection connection = exchange.getConnection();
    this.pooled = connection.getByteBufferPool().allocate();
    this.bufferSize = connection.getBufferSize();

    onComplete(exchange, sender);
  }

  @Override
  public void run() {
    ByteBuffer buffer = pooled.getBuffer();
    chunk += 1;
    try {
      buffer.clear();
      int count = source.read(buffer);
      if (count == -1) {
        done();
        callback.onComplete(exchange, sender);
      } else {
        if (chunk == 1) {
          if (count < bufferSize) {
            HeaderMap headers = exchange.getResponseHeaders();
            if (!headers.contains(Headers.CONTENT_LENGTH)) {
              headers.put(Headers.CONTENT_LENGTH, count);
              headers.remove(Headers.TRANSFER_ENCODING);
            }
          } else {
            HeaderMap headers = exchange.getResponseHeaders();
            // just check if
            if (!headers.contains(Headers.CONTENT_LENGTH)) {
              headers.put(Headers.TRANSFER_ENCODING, "chunked");
            }
          }
        }
        buffer.flip();
        sender.send(buffer, this);
      }
    } catch (IOException ex) {
      onException(exchange, sender, ex);
    }
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
    } else {
      run();
    }
  }

  @Override
  public void onException(final HttpServerExchange exchange, final Sender sender,
      final IOException ex) {
    done();
    callback.onException(exchange, sender, ex);
  }

  private void done() {
    pooled.close();
    pooled = null;
    IoUtils.safeClose(source);
  }

}
