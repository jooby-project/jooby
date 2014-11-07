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
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.WebSocket;
import org.jooby.fn.ExSupplier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;

public class WebSocketImpl implements WebSocket {

  @SuppressWarnings({"rawtypes", "serial" })
  private static final Callback NOOP = new Callback() {
    @Override
    public void invoke(final Object arg) throws Exception {
    }
  };

  private String path;

  private String pattern;

  private String name;

  private Map<String, String> vars;

  private MediaType consumes;

  private MediaType produces;

  private Handler handler;

  private Callback<Mutant> messageCallback = noop();

  private Callback<CloseStatus> closeCallback = noop();

  private Callback<Exception> exceptionCallback = noop();

  private Session session;

  private Injector injector;

  private SuspendToken suspendToken;

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

  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public void close(final CloseStatus status) {
    session.close(status.code(), status.reason());
  }

  @Override
  public void resume() {
    if (suspendToken != null) {
      suspendToken.resume();
    }
  }

  @Override
  public void pause() {
    if (suspendToken == null) {
      suspendToken = session.suspend();
    }
  }

  @Override
  public void terminate() throws Exception {
    session.disconnect();
  }

  @Override
  public void send(final Object data, final Callback0 success, final Callback<Exception> err)
      throws Exception {
    requireNonNull(data, "A data message is required.");
    requireNonNull(success, "Success callback is required.");
    requireNonNull(err, "Error callback is required.");
    WriteCallback callback = new WriteCallback() {
      @Override
      public void writeSuccess() {
        try {
          success.invoke();
        } catch (Exception ex) {
          log.debug("Error while invoking write success callback", ex);
        }
      }

      @Override
      public void writeFailed(final Throwable cause) {
        if (cause instanceof Exception) {
          try {
            err.invoke((Exception) cause);
          } catch (Exception ex) {
            log.debug("Error while invoking write failed callback", ex);
          }
        } else {
          log.error("Serious error found while invoking write failed callback", cause);
        }
      }
    };

    Optional<Body.Formatter> converter = injector.getInstance(BodyConverterSelector.class)
        .forWrite(data, ImmutableList.of(produces));
    if (converter.isPresent()) {
      ExSupplier<OutputStream> stream = () -> {
        return stream(session, callback, false);
      };
      ExSupplier<Writer> reader = () -> {
        return new PrintWriter(stream(session, callback, true));
      };
      converter.get().format(data, new BodyWriterImpl(Charsets.UTF_8, stream, reader));
    } else {
      RemoteEndpoint remote = session.getRemote();
      if (byte[].class == data.getClass() || Byte[].class == data.getClass()) {
        remote.sendBytes(ByteBuffer.wrap((byte[]) data), callback);
      } else if (ByteBuffer.class.isInstance(data)) {
        remote.sendBytes((ByteBuffer) data, callback);
      } else {
        // TODO: complete me!
        remote.sendString(data.toString(), callback);
      }
    }
  }

  private static OutputStream stream(final Session session, final WriteCallback callback,
      final boolean text) {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        RemoteEndpoint remote = session.getRemote();
        // TODO: auto handle partial content?
        if (text) {
          remote.sendString(toString(), callback);
        } else {
          // binary
          remote.sendBytes(ByteBuffer.wrap(toByteArray()), callback);
        }
      }
    };
  }

  @Override
  public void onMessage(final Callback<Mutant> callback) throws Exception {
    this.messageCallback = requireNonNull(callback, "Message callback is required.");
  }

  public void connect(final Injector injector, final Session session) throws Exception {
    this.injector = requireNonNull(injector, "The injector is required.");
    this.session = requireNonNull(session, "The session is required.");
    handler.connect(this);
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String pattern() {
    return pattern.substring(pattern.indexOf('/'));
  }

  @Override
  public String name() {
    return name;
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
  public <T> T getInstance(final Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("WS ").append(path()).append("\n");
    buffer.append("  pattern: ").append(pattern()).append("\n");
    buffer.append("  name: ").append(name()).append("\n");
    buffer.append("  vars: ").append(vars()).append("\n");
    buffer.append("  consumes: ").append(consumes()).append("\n");
    buffer.append("  produces: ").append(produces()).append("\n");
    return buffer.toString();
  }

  public void fireMessage(final Mutant variant) throws Exception {
    this.messageCallback.invoke(variant);
  }

  public void fireErr(final Exception cause) throws Exception {
    exceptionCallback.invoke(cause);
  }

  public void fireClose(final CloseStatus closeStatus) throws Exception {
    closeCallback.invoke(closeStatus);
  }

  @Override
  public void onError(final Callback<Exception> callback) throws Exception {
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
