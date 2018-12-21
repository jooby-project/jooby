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
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;

import static io.jooby.Throwing.throwingConsumer;

public class UtowContext extends BaseContext implements IoCallback {

  private Route route;
  private HttpServerExchange exchange;
  private Router router;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<Closeable> resources;
  private Value.Object headers;
  private Map<String, String> pathMap = Collections.emptyMap();

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
    requireBlocking();
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
    return Body.of(exchange.getInputStream(), exchange.getRequestContentLength());
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
      try (FormDataParser parser = new FormEncodedDataDefinition()
          .setDefaultEncoding(StandardCharsets.UTF_8.name())
          .create(exchange)) {
        formData(form, parser.parseBlocking());
      } catch (Exception x) {
        throw Throwing.sneakyThrow(x);
      }
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    requireBlocking();
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      if (!exchange.isBlocking()) {
        exchange.startBlocking();
      }
      try {
        FormDataParser parser = new MultiPartParserDefinition()
            .setDefaultEncoding(StandardCharsets.UTF_8.name())
            .setTempFileLocation(router.tmpdir())
            .create(exchange);

        register(parser);

        formData(multipart, parser.parseBlocking());
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
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

  @Nonnull @Override public Context outputStream(Throwing.Consumer<OutputStream> consumer)
      throws Exception {
    requireBlocking();
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
    try (OutputStream output = exchange.getOutputStream()) {
      consumer.accept(output);
    }
    return this;
  }

  @Nonnull @Override public Context responseChannel(Throwing.Consumer<WritableByteChannel> consumer)
      throws Exception {
    try (UtowResponseChannel channel = new UtowResponseChannel(exchange.getResponseChannel())) {
      consumer.accept(channel);
    }
    return this;
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

  private void destroy(HttpServerExchange exchange) {
    try {
      if (resources != null) {
        // TODO: use a log
        resources.forEach(throwingConsumer(Closeable::close));
        resources = null;
      }
    } finally {
      exchange.endExchange();
    }
    this.router = null;
    this.route = null;
    this.exchange = null;
  }

  private Closeable register(Closeable closeable) {
    if (resources == null) {
      resources = new ArrayList<>();
    }
    resources.add(closeable);
    return closeable;
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
    destroy(exchange);
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    destroy(exchange);
  }
}
