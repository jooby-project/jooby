/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.Body;
import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.Value;
import io.jooby.ValueNode;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.SameThreadExecutor;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;
import static io.undertow.util.Headers.CONTENT_LENGTH;
import static io.undertow.util.Headers.CONTENT_TYPE;
import static io.undertow.util.Headers.RANGE;
import static io.undertow.util.Headers.SET_COOKIE;

public class UtowContext implements DefaultContext, IoCallback {

  private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);
  private Route route;
  private HttpServerExchange exchange;
  private Router router;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private ValueNode headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> attributes;
  Body body;
  private MediaType responseType;
  private Map<String, String> cookies;
  private HashMap<String, String> responseCookies;
  private long responseLength = -1;
  private Boolean resetHeadersOnError;

  public UtowContext(HttpServerExchange exchange, Router router) {
    this.exchange = exchange;
    this.router = router;
  }

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  @Nonnull @Override public Body body() {
    return body == null ? Body.empty(this) : body;
  }

  @Override public @Nonnull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      Collection<io.undertow.server.handlers.Cookie> cookies = exchange.getRequestCookies()
          .values();
      if (cookies.size() > 0) {
        this.cookies = new LinkedHashMap<>(cookies.size());
        for (io.undertow.server.handlers.Cookie it : cookies) {
          this.cookies.put(it.getName(), it.getValue());
        }
      } else {
        this.cookies = Collections.emptyMap();
      }
    }
    return cookies;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
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
    return exchange.getSourceAddress().getHostString();
  }

  @Nonnull @Override public String getProtocol() {
    return exchange.getProtocol().toString();
  }

  @Nonnull @Override public String getScheme() {
    String scheme = exchange.getRequestScheme();
    return scheme == null ? "http" : scheme.toLowerCase();
  }

  @Nonnull @Override public ValueNode header(@Nonnull String name) {
    return Value.create(this, name, exchange.getRequestHeaders().get(name));
  }

  @Nonnull @Override public ValueNode header() {
    HeaderMap map = exchange.getRequestHeaders();
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      Collection<HttpString> names = map.getHeaderNames();
      for (HttpString name : names) {
        HeaderValues values = map.get(name);
        headerMap.put(name.toString(), values);
      }
      headers = Value.hash(this, headerMap);
    }
    return headers;
  }

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      query = QueryString.create(this, exchange.getQueryString());
    }
    return query;
  }

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = Formdata.create(this);
      formData(form, exchange.getAttachment(FORM_DATA));
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = Multipart.create(this);
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

  @Nonnull @Override public Context detach(@Nonnull Route.Handler next) {
    exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
      try {
        next.apply(this);
      } catch (Throwable x) {
        sendError(x);
      }
    });
    return this;
  }

  @Nonnull @Override public StatusCode getResponseCode() {
    return StatusCode.valueOf(exchange.getStatusCode());
  }

  @Nonnull @Override public Context setResponseCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    return this;
  }

  @Nonnull @Override public Context removeResponseHeader(@Nonnull String name) {
    exchange.getResponseHeaders().remove(name);
    return this;
  }

  @Nonnull @Override public Context removeResponseHeaders() {
    exchange.getResponseHeaders().clear();
    return this;
  }

  @Nonnull @Override public MediaType getResponseType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Nonnull @Override public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    if (responseType == null) {
      setResponseType(contentType, contentType.getCharset());
    }
    return this;
  }

  @Nonnull @Override
  public Context setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    this.responseType = contentType;
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType);
    return this;
  }

  @Nonnull @Override public Context setResponseLength(long length) {
    responseLength = length;
    exchange.getResponseHeaders().put(CONTENT_LENGTH, Long.toString(length));
    return this;
  }

  @Override public long getResponseLength() {
    return responseLength;
  }

  @Nonnull public Context setResponseCookie(@Nonnull Cookie cookie) {
    if (responseCookies == null) {
      responseCookies = new HashMap<>();
    }
    cookie.setPath(cookie.getPath(getContextPath()));
    responseCookies.put(cookie.getName(), cookie.toCookieString());
    HeaderMap headers = exchange.getResponseHeaders();
    headers.remove(SET_COOKIE);
    for (String cookieString : responseCookies.values()) {
      headers.add(SET_COOKIE, cookieString);
    }
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

    setResponseType(type, charset);
    ifSetChunked();

    return new PrintWriter(new UtowWriter(exchange.getOutputStream(), charset));
  }

  @Nonnull @Override public Context send(@Nonnull byte[] data) {
    return send(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context send(@Nonnull ReadableByteChannel channel) {
    ifSetChunked();
    new UtowChunkedStream(exchange.getRequestContentLength()).send(channel, exchange, this);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    return send(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, Long.toString(data.remaining()));
    exchange.getResponseSender().send(data, this);
    return this;
  }

  @Nonnull @Override public Context send(StatusCode statusCode) {
    exchange.setStatusCode(statusCode.value());
    exchange.getResponseSender().send(EMPTY, this);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return send(((FileInputStream) in).getChannel());
    }
    try {
      ifSetChunked();
      long len = exchange.getResponseContentLength();
      ByteRange range = ByteRange
          .parse(exchange.getRequestHeaders().getFirst(RANGE), len)
          .apply(this);
      new UtowChunkedStream(len).send(Channels.newChannel(range.apply(in)), exchange, this);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Nonnull @Override public Context send(@Nonnull FileChannel file) {
    try {
      long len = file.size();
      exchange.setResponseContentLength(len);
      ByteRange range = ByteRange
          .parse(exchange.getRequestHeaders().getFirst(RANGE), len)
          .apply(this);
      file.position(range.getStart());
      new UtowChunkedStream(range.getEnd()).send(file, exchange, this);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public boolean isResponseStarted() {
    return exchange.isResponseStarted();
  }

  @Override public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().getResetHeadersOnError()
        : resetHeadersOnError.booleanValue();
  }

  @Override public Context setResetHeadersOnError(boolean value) {
    this.resetHeadersOnError = value;
    return this;
  }

  @Override public void onComplete(HttpServerExchange exchange, Sender sender) {
    ifSaveSession();
    destroy(null);
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    destroy(exception);
  }

  @Override public String toString() {
    return getMethod() + " " + pathString();
  }

  private void ifSaveSession() {
    if (attributes != null) {
      Session session = (Session) attributes.get(Session.NAME);
      if (session != null && (session.isNew() || session.isModify())) {
        SessionStore store = getRouter().getSessionOptions().getStore();
        store.save(session);
      }
    }
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
    } finally {
      this.exchange.endExchange();
    }
  }

  private void formData(Formdata form, FormData data) {
    if (data != null) {
      Iterator<String> it = data.iterator();
      while (it.hasNext()) {
        String path = it.next();
        Deque<FormData.FormValue> values = data.get(path);
        for (FormData.FormValue value : values) {
          if (value.isFileItem()) {
            ((Multipart) form).put(path, new UtowFileUpload(value));
          } else {
            form.put(path, value.getValue());
          }
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
