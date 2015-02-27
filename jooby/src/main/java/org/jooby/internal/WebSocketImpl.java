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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.jooby.Body;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.WebSocket;
import org.jooby.fn.ExSupplier;
import org.jooby.internal.reqparam.RootParamConverter;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private NativeWebSocket ws;

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
    ws.close(status.code(), status.reason());
  }

  @Override
  public void resume() {
    if (suspended) {
      ws.resume();
      suspended = false;
    }
  }

  @Override
  public void pause() {
    if (!suspended) {
      ws.pause();
      suspended = true;
    }
  }

  @Override
  public void terminate() throws Exception {
    ws.terminate();
  }

  @Override
  public void send(final Object data, final SuccessCallback success, final ErrCallback err)
      throws Exception {
    requireNonNull(data, "A data message is required.");
    requireNonNull(success, "A success callback is required.");
    requireNonNull(err, "An error callback is required.");

    Optional<Body.Formatter> formatter = injector.getInstance(BodyConverterSelector.class)
        .formatter(data, ImmutableList.of(produces));
    if (formatter.isPresent()) {
      ExSupplier<OutputStream> stream = () -> stream(ws, success, err, false);
      ExSupplier<Writer> writer = () -> new OutputStreamWriter(stream(ws, success, err, true),
          Charsets.UTF_8);
      formatter.get().format(data, new BodyWriterImpl(Charsets.UTF_8, stream, writer));
    } else {
      // TODO: complete me!
      ws.send(data.toString(), success, err);
    }
  }

  @Override
  public void onMessage(final Callback<Mutant> callback) throws Exception {
    this.messageCallback = requireNonNull(callback, "Message callback is required.");
  }

  public void connect(final Injector injector, final NativeWebSocket ws) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.ws = requireNonNull(ws, "Web socket is required.");

    /**
     * Bind callbacks
     */
    ws.onBinaryMessage(buffer -> {
      try {
        messageCallback.invoke(new WsBinaryMessage(buffer));
      } catch (Throwable ex) {
        handleErr(ex);
      }
    });
    ws.onTextMessage(message -> {
      try {
        messageCallback.invoke(
            new MutantImpl(injector.getInstance(RootParamConverter.class), new Object[]{message })
            );
      } catch (Throwable ex) {
        handleErr(ex);
      }
    });
    ws.onCloseMessage((code, reason) -> {
      try {
        closeCallback.invoke(reason.map(r -> WebSocket.CloseStatus.of(code, r)).orElse(
            WebSocket.CloseStatus.of(code)));
      } catch (Throwable ex) {
        handleErr(ex);
      }
    });
    ws.onErrorMessage(cause -> handleErr(cause));

    // connect now
    try {
      handler.connect(this);
    } catch (Throwable ex) {
      handleErr(ex);
    }
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

  private void handleErr(final Throwable cause) {
    try {
      exceptionCallback.invoke(cause);
    } finally {
      cleanup(cause);
    }
  }

  private void cleanup(final Throwable cause) {
    NativeWebSocket lws = ws;
    this.ws = null;
    this.injector = null;
    this.handler = null;
    this.closeCallback = null;
    this.exceptionCallback = null;
    this.messageCallback = null;

    if (lws.isOpen()) {
      WebSocket.CloseStatus closeStatus = WebSocket.SERVER_ERROR;
      if (cause instanceof IllegalArgumentException) {
        closeStatus = WebSocket.BAD_DATA;
      } else if (cause instanceof NoSuchElementException) {
        closeStatus = WebSocket.BAD_DATA;
      } else if (cause instanceof Err) {
        Err err = (Err) cause;
        if (err.statusCode() == 400) {
          closeStatus = WebSocket.BAD_DATA;
        }
      }
      lws.close(closeStatus.code(), closeStatus.reason());
    }
  }

  private static OutputStream stream(final NativeWebSocket ws, final SuccessCallback success,
      final ErrCallback err, final boolean text) {
    return new ByteArrayOutputStream(1024) {
      @Override
      public void close() throws IOException {
        // TODO: auto handle partial content?
        if (text) {
          ws.send(new String(buf, 0, count, Charsets.UTF_8), success, err);
        } else {
          ws.send(ByteBuffer.wrap(buf, 0, count), success, err);
        }
      }
    };
  }

}
