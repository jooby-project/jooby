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
package org.jooby.internal;

import static java.util.Objects.requireNonNull;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.WebSocket;
import org.jooby.fn.ExSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;

public class WebSocketImpl implements WebSocket {

  @SuppressWarnings({"rawtypes" })
  private static final Callback NOOP = arg -> {
  };

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(WebSocket.class);

  private String path;

  private String pattern;

  private Map<String, String> vars;

  private MediaType consumes;

  private MediaType produces;

  private Handler handler;

  private Callback<Mutant> messageCallback = noop();

  private Callback<CloseStatus> closeCallback = noop();

  private ErrCallback exceptionCallback = cause -> {
    log.error("execution of WS" + path() + " resulted in exception", cause);
  };

  private WebSocketChannel channel;

  private Injector injector;

  private boolean suspended;

  public WebSocketImpl(final Handler handler, final String path,
      final String pattern, final Map<String, String> vars,
      final MediaType consumes, final MediaType produces) {
    this.handler = handler;
    this.path = path;
    this.pattern = pattern;
    this.vars = vars;
    this.consumes = consumes;
    this.produces = produces;
  }

  @Override
  public void close(final CloseStatus status) {
    WebSockets.sendClose(status.code(), status.reason(), channel, new WebSocketCallback<Void>() {

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
    if (suspended) {
      channel.resumeReceives();
      suspended = false;
    }
  }

  @Override
  public void pause() {
    if (!suspended) {
      channel.suspendReceives();
      suspended = true;
    }
  }

  @Override
  public void terminate() throws Exception {
    channel.close();
  }

  @Override
  public void send(final Object data, final SuccessCallback success, final ErrCallback err)
      throws Exception {
    requireNonNull(data, "A data message is required.");
    requireNonNull(success, "A success callback is required.");
    requireNonNull(err, "An error callback is required.");
    WebSocketCallback<Void> callback = new WebSocketCallback<Void>() {
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

    Optional<Body.Formatter> formatter = injector.getInstance(BodyConverterSelector.class)
        .formatter(data, ImmutableList.of(produces));
    if (formatter.isPresent()) {
      ExSupplier<OutputStream> stream = () -> {
        return stream(channel, callback, false);
      };
      ExSupplier<Writer> writer = () -> {
        return new PrintWriter(stream(channel, callback, true));
      };
      formatter.get().format(data, new BodyWriterImpl(Charsets.UTF_8, stream, writer));
    } else {
      // TODO: complete me!
      WebSockets.sendText(data.toString(), channel, callback);
    }
  }

  private static OutputStream stream(final WebSocketChannel channel,
      final WebSocketCallback<Void> callback,
      final boolean text) {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        // TODO: auto handle partial content?
        if (text) {
          WebSockets.sendText(toString(), channel, callback);
        } else {
          // binary
          WebSockets.sendBinary(ByteBuffer.wrap(toByteArray()), channel, callback);
        }
      }
    };
  }

  @Override
  public void onMessage(final Callback<Mutant> callback) throws Exception {
    this.messageCallback = requireNonNull(callback, "Message callback is required.");
  }

  public void connect(final Injector injector, final WebSocketChannel channel) throws Exception {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.channel = requireNonNull(channel, "A channel is required.");
    handler.connect(this);
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String pattern() {
    return pattern;
  }

  @Override
  public Map<String, String> vars() {
    return vars;
  }

  @Override
  public MediaType consumes() {
    return consumes;
  }

  @Override
  public MediaType produces() {
    return produces;
  }

  @Override
  public <T> T require(final Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("WS ").append(path()).append("\n");
    buffer.append("  pattern: ").append(pattern()).append("\n");
    buffer.append("  vars: ").append(vars()).append("\n");
    buffer.append("  consumes: ").append(consumes()).append("\n");
    buffer.append("  produces: ").append(produces()).append("\n");
    return buffer.toString();
  }

  public void fireMessage(final Mutant variant) throws Exception {
    this.messageCallback.invoke(variant);
  }

  public void fireErr(final Throwable cause) {
    exceptionCallback.invoke(cause);
  }

  public void fireClose(final CloseStatus closeStatus) throws Exception {
    try {
      closeCallback.invoke(closeStatus);
    } finally {
      channel = null;
      injector = null;
    }
  }

  @Override
  public void onError(final WebSocket.ErrCallback callback) {
    this.exceptionCallback = requireNonNull(callback, "A callback is required.");
  }

  @Override
  public void onClose(final Callback<CloseStatus> callback) throws Exception {
    this.closeCallback = requireNonNull(callback, "A callback is required.");
  }

  @SuppressWarnings("unchecked")
  private static <T> Callback<T> noop() {
    return NOOP;
  }
}
