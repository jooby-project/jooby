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
import io.jooby.ByteRange;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.*;
import io.undertow.util.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;
import static io.undertow.util.Headers.CONTENT_TYPE;
import static io.undertow.util.Headers.RANGE;

public class UtowContext implements Context, IoCallback {

  private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);
  private Route route;
  private HttpServerExchange exchange;
  private Router router;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private Value headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> attributes = new HashMap<>();
  Body body;
  private MediaType responseType;

  public UtowContext(HttpServerExchange exchange, Router router) {
    this.exchange = exchange;
    this.router = router;
  }

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  @Nonnull @Override public Body body() {
    return body == null ? Body.empty() : body;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Nonnull @Override public String getMethod() {
    return exchange.getRequestMethod().toString().toUpperCase();
  }

  @Nonnull @Override public Route getRoute() {
    return route;
  }

  @Nonnull @Override public Context setRoute(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return exchange.getRequestPath();
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public Context setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override public boolean isInIoThread() {
    return exchange.isInIoThread();
  }

  @Nonnull @Override public String getRemoteAddress() {
    return exchange.getSourceAddress().getHostName();
  }

  @Nonnull @Override public String getProtocol() {
    return exchange.getProtocol().toString();
  }

  @Nonnull @Override public String getScheme() {
    String scheme = exchange.getRequestScheme();
    return scheme == null ? "http" : scheme.toLowerCase();
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(name, exchange.getRequestHeaders().get(name));
  }

  @Nonnull @Override public Value headers() {
    HeaderMap map = exchange.getRequestHeaders();
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      Collection<HttpString> names = map.getHeaderNames();
      for (HttpString name : names) {
        HeaderValues values = map.get(name);
        headerMap.put(name.toString(), values);
      }
      headers = Value.hash(headerMap);
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
    return dispatch(router.getWorker(), action);
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

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.valueOf(exchange.getStatusCode());
  }

  @Nonnull @Override public Context setStatusCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public Context setHeader(@Nonnull String name, @Nonnull String value) {
    exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    return this;
  }

  @Nonnull @Override public MediaType getResponseContentType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Nonnull @Override public Context setDefaultContentType(@Nonnull MediaType contentType) {
    if (responseType == null) {
      setContentType(contentType, contentType.getCharset());
    }
    return this;
  }

  @Nonnull @Override
  public Context setContentType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    this.responseType = contentType;
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @Nonnull @Override public Context setContentType(@Nonnull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType);
    return this;
  }

  @Nonnull @Override public Context setContentLength(long length) {
    exchange.setResponseContentLength(length);
    return this;
  }

  @Nonnull @Override public OutputStream responseStream() {
    ifStartBlocking();

    ifSetChunked();

    return exchange.getOutputStream();
  }

  @Nonnull @Override public io.jooby.Sender responseSender() {
    return new UtowSender(this, exchange);
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    ifStartBlocking();

    setContentType(type, charset);
    ifSetChunked();

    return new PrintWriter(new UtowWriter(exchange.getOutputStream(), charset));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    return sendBytes(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ReadableByteChannel channel) {
    ifSetChunked();
    new UtowChunkedStream(exchange.getRequestContentLength()).send(channel, exchange, this);
    return this;
  }

  @Nonnull @Override public Context sendString(@Nonnull String data, @Nonnull Charset charset) {
    return sendBytes(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    exchange.setResponseContentLength(data.remaining());
    exchange.getResponseSender().send(data, this);
    return this;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    exchange.setResponseContentLength(0);
    exchange.setStatusCode(statusCode);
    exchange.getResponseSender().send(EMPTY, this);
    return this;
  }

  @Nonnull @Override public Context sendStream(@Nonnull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return sendFile(((FileInputStream) in).getChannel());
    }
    try {
      ifSetChunked();
      long len = exchange.getResponseContentLength();
      if (len > 0) {
        ByteRange range = ByteRange.parse(exchange.getRequestHeaders().getFirst(RANGE))
            .apply(this, len);
        in.skip(range.start);
        len = range.end;
      } else {
        len = -1;
      }
      new UtowChunkedStream(len).send(Channels.newChannel(in), exchange, this);
      return this;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Context sendFile(@Nonnull FileChannel file) {
    try {
      long len = file.size();
      exchange.setResponseContentLength(len);
      ByteRange range = ByteRange.parse(exchange.getRequestHeaders().getFirst(RANGE))
          .apply(this, len);
      file.position(range.start);
      new UtowChunkedStream(range.end).send(file, exchange, this);
      return this;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public boolean isResponseStarted() {
    return exchange.isResponseStarted();
  }

  @Override public void onComplete(HttpServerExchange exchange, Sender sender) {
    destroy(null);
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    destroy(exception);
  }

  void destroy(Exception cause) {
    try {
      if (cause != null) {
        Logger log = router.getLog();
        if (Server.connectionLost(cause)) {
          log.debug("exception found while sending response {} {}", getMethod(), pathString(),
              cause);
        } else {
          log.error("exception found while sending response {} {}", getMethod(), pathString(),
              cause);
        }
      }
      this.router = null;
      this.route = null;
    } finally {
      this.exchange.endExchange();
      this.exchange = null;
    }
  }

  private void formData(Formdata form, FormData data) {
    Iterator<String> it = data.iterator();
    while (it.hasNext()) {
      String path = it.next();
      Deque<FormData.FormValue> values = data.get(path);
      for (FormData.FormValue value : values) {
        if (value.isFileItem()) {
          form.put(path, new UtowFileUpload(path, value));
        } else {
          form.put(path, value.getValue());
        }
      }
    }
  }

  private void ifSetChunked() {
    HeaderMap responseHeaders = exchange.getResponseHeaders();
    if (!responseHeaders.contains(Headers.CONTENT_LENGTH)) {
      exchange.getResponseHeaders().put(Headers.TRANSFER_ENCODING, Headers.CHUNKED.toString());
    }
  }

  private void ifStartBlocking() {
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
  }
}
