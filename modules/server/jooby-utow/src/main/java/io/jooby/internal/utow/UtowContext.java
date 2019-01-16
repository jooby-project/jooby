/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.*;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.*;
import io.undertow.util.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;

public class UtowContext implements Context, IoCallback {

  private Route route;
  private HttpServerExchange exchange;
  private Router router;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private Value.Object headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> locals = Collections.EMPTY_MAP;
  Body body;

  public UtowContext(HttpServerExchange exchange, Router router) {
    this.exchange = exchange;
    this.router = router;
  }

  @Nonnull @Override public Router router() {
    return router;
  }

  @Override public String name() {
    return "utow";
  }

  @Nonnull @Override public Body body() {
    return body == null ? Body.empty() : body;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nullable @Override public <T> T get(String name) {
    return (T) locals.get(name);
  }

  @Nonnull @Override public Context set(@Nonnull String name, @Nonnull Object value) {
    if (locals == Collections.EMPTY_MAP) {
      locals = new HashMap<>();
    }
    locals.put(name, value);
    return this;
  }

  @Nonnull @Override public String method() {
    return exchange.getRequestMethod().toString().toUpperCase();
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public Context route(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return exchange.getRequestPath();
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public Context pathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override public boolean isInIoThread() {
    return exchange.isInIoThread();
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(name, exchange.getRequestHeaders().get(name));
  }

  @Nonnull @Override public Value headers() {
    HeaderMap map = exchange.getRequestHeaders();
    if (headers == null) {
      headers = Value.headers();
      Collection<HttpString> names = map.getHeaderNames();
      for (HttpString name : names) {
        HeaderValues values = map.get(name);
        headers.put(name.toString(), values);
      }
    }
    return headers;
  }

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      String queryString = exchange.getQueryString();
      if (queryString.length() == 0) {
        return QueryString.EMPTY;
      }
      query = Value.queryString('?' + queryString);
    }
    return query;
  }

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = new Formdata();
      formData(form, exchange.getAttachment(FORM_DATA));
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      formData(multipart, exchange.getAttachment(FORM_DATA));
    }
    return multipart;
  }

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(router.worker(), action);
  }

  @Nonnull @Override public Context dispatch(@Nonnull Executor executor,
      @Nonnull Runnable action) {
    exchange.dispatch(executor, action);
    return this;
  }

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    exchange.dispatch(SameThreadExecutor.INSTANCE, action);
    return this;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public Context header(@Nonnull String name, @Nonnull String value) {
    exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType, @Nullable String charset) {
    if (charset == null) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    } else {
      exchange.getResponseHeaders()
          .put(Headers.CONTENT_TYPE, contentType + ";charset=" + charset);
    }
    return this;
  }

  @Nonnull @Override public Context length(long length) {
    exchange.setResponseContentLength(length);
    return this;
  }

  @Nonnull @Override public OutputStream responseStream() {
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }

    HeaderMap responseHeaders = exchange.getResponseHeaders();
    if (!responseHeaders.contains(Headers.CONTENT_LENGTH)) {
      exchange.getResponseHeaders().put(Headers.TRANSFER_ENCODING, Headers.CHUNKED.toString());
    }

    return exchange.getOutputStream();
  }

  @Nonnull @Override public Writer responseWriter(MediaType type, Charset charset) {
    OutputStream outputStream = responseStream();

    type(type.value(), charset.name());
    UtowWriter writer = new UtowWriter(outputStream, charset);
    return writer;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    return sendBytes(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    return sendBytes(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    exchange.setResponseContentLength(data.remaining());
    exchange.getResponseSender().send(data, this);
    return this;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    exchange.setResponseContentLength(0);
    exchange.setStatusCode(statusCode).endExchange();
    return this;
  }

  @Override public boolean isResponseStarted() {
    return exchange.isResponseStarted();
  }

  private void destroy(Exception exception) {
    if (exception != null) {
      Logger log = router.log();
      if (Server.connectionLost(exception)) {
        log.debug("%s %s", method(), pathString(), exception);
      } else {
        log.error("exception found while sending response {} {} {}", method(), pathString(),
            exception);
      }
    }
    this.router = null;
    this.route = null;
    this.exchange.endExchange();
    this.exchange = null;
  }

  private void formData(Formdata form, FormData data) {
    Iterator<String> it = data.iterator();
    while (it.hasNext()) {
      String path = it.next();
      Deque<FormData.FormValue> values = data.get(path);
      for (FormData.FormValue value : values) {
        if (value.isFile()) {
          form.put(path, new UtowFileUpload(path, value));
        } else {
          form.put(path, value.getValue());
        }
      }
    }
  }

  @Override public void onComplete(HttpServerExchange exchange, Sender sender) {
    destroy(null);
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    destroy(exception);
  }
}
