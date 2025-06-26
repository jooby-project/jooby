/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;
import static io.undertow.util.Headers.CONTENT_LENGTH;
import static io.undertow.util.Headers.CONTENT_TYPE;
import static io.undertow.util.Headers.RANGE;
import static io.undertow.util.Headers.SET_COOKIE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOption;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import io.jooby.output.Output;
import io.jooby.value.Value;
import io.undertow.Handlers;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RenegotiationRequiredException;
import io.undertow.server.SSLSessionInfo;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.*;

public class UndertowContext implements DefaultContext, IoCallback {
  private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);
  private Route route;
  HttpServerExchange exchange;
  private Router router;
  private QueryString query;
  private Formdata formdata;
  private Value headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> attributes;
  Body body;
  private MediaType responseType;
  private Map<String, String> cookies;
  private HashMap<String, String> responseCookies;
  private long responseLength = -1;
  private Boolean resetHeadersOnError;
  private String method;
  private String requestPath;
  private UndertowCompletionListener completionListener;
  private String remoteAddress;
  private String host;
  private int port;

  public UndertowContext(HttpServerExchange exchange, Router router) {
    this.exchange = exchange;
    this.router = router;
    this.method = exchange.getRequestMethod().toString().toUpperCase();
    this.requestPath = exchange.getRequestPath();
  }

  boolean isHttpGet() {
    return this.method.length() == 3
        && this.method.charAt(0) == 'G'
        && this.method.charAt(1) == 'E'
        && this.method.charAt(2) == 'T';
  }

  @NonNull @Override
  public Router getRouter() {
    return router;
  }

  @NonNull @Override
  public Body body() {
    return body == null ? Body.empty(this) : body;
  }

  @Override
  public @NonNull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      this.cookies = new LinkedHashMap<>();
      for (var it : exchange.requestCookies()) {
        this.cookies.put(it.getName(), it.getValue());
      }
    }
    return cookies;
  }

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    return attributes;
  }

  @NonNull @Override
  public String getMethod() {
    return method;
  }

  @NonNull @Override
  public Context setMethod(@NonNull String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @NonNull @Override
  public Route getRoute() {
    return route;
  }

  @NonNull @Override
  public Context setRoute(Route route) {
    this.route = route;
    return this;
  }

  @NonNull @Override
  public String getRequestPath() {
    return requestPath;
  }

  @NonNull @Override
  public Context setRequestPath(@NonNull String path) {
    this.requestPath = path;
    return this;
  }

  @NonNull @Override
  public Map<String, String> pathMap() {
    return pathMap;
  }

  @NonNull @Override
  public Context setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override
  public boolean isInIoThread() {
    return exchange.isInIoThread();
  }

  @NonNull @Override
  public String getHost() {
    return host == null ? DefaultContext.super.getHost() : host;
  }

  @NonNull @Override
  public Context setHost(@NonNull String host) {
    this.host = host;
    return this;
  }

  @NonNull @Override
  public String getRemoteAddress() {
    if (remoteAddress == null) {
      String remoteAddr =
          Optional.ofNullable(exchange.getSourceAddress())
              .map(InetSocketAddress::getHostString)
              .orElse("")
              .trim();
      return remoteAddr;
    }
    return remoteAddress;
  }

  @NonNull @Override
  public Context setRemoteAddress(@NonNull String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @NonNull @Override
  public String getProtocol() {
    return exchange.getProtocol().toString();
  }

  @NonNull @Override
  public List<Certificate> getClientCertificates() {
    SSLSessionInfo ssl = exchange.getConnection().getSslSessionInfo();
    if (ssl != null) {
      try {
        return Arrays.asList(ssl.getPeerCertificates());
      } catch (SSLPeerUnverifiedException | RenegotiationRequiredException x) {
        throw SneakyThrows.propagate(x);
      }
    }
    return Collections.emptyList();
  }

  @NonNull @Override
  public String getScheme() {
    String scheme = exchange.getRequestScheme();
    return scheme == null ? "http" : scheme.toLowerCase();
  }

  @NonNull @Override
  public Context setScheme(@NonNull String scheme) {
    exchange.setRequestScheme(scheme);
    return this;
  }

  @Override
  public int getPort() {
    return port > 0 ? port : DefaultContext.super.getPort();
  }

  @NonNull @Override
  public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @NonNull @Override
  public Value header(@NonNull String name) {
    return Value.create(
        getRouter().getValueFactory(), name, exchange.getRequestHeaders().get(name));
  }

  @NonNull @Override
  public Value header() {
    HeaderMap map = exchange.getRequestHeaders();
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      Collection<HttpString> names = map.getHeaderNames();
      for (HttpString name : names) {
        HeaderValues values = map.get(name);
        headerMap.put(name.toString(), values);
      }
      headers = Value.headers(getRouter().getValueFactory(), headerMap);
    }
    return headers;
  }

  @NonNull @Override
  public QueryString query() {
    if (query == null) {
      query = QueryString.create(getRouter().getValueFactory(), exchange.getQueryString());
    }
    return query;
  }

  @NonNull @Override
  public Formdata form() {
    if (formdata == null) {
      formdata = Formdata.create(getRouter().getValueFactory());
      formData(formdata, exchange.getAttachment(FORM_DATA));
    }
    return formdata;
  }

  @NonNull @Override
  public Context dispatch(@NonNull Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @NonNull @Override
  public Context dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    exchange.dispatch(executor, action);
    return this;
  }

  @NonNull @Override
  public Context detach(@NonNull Route.Handler next) throws Exception {
    exchange.dispatch(SameThreadExecutor.INSTANCE, detach(this, next));
    return this;
  }

  private static Runnable detach(Context ctx, Route.Handler next) {
    return () -> {
      try {
        next.apply(ctx);
      } catch (Exception cause) {
        ctx.sendError(cause);
      }
    };
  }

  @NonNull @Override
  public Context upgrade(@NonNull WebSocket.Initializer handler) {
    try {
      Handlers.websocket(
              (exchange, channel) -> {
                UndertowWebSocket ws = new UndertowWebSocket(this, channel);
                handler.init(Context.readOnly(this), ws);
                ws.fireConnect();
              })
          .handleRequest(exchange);
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
    try {
      handler.handle(new UndertowSeverSentEmitter(this));
    } catch (Throwable x) {
      sendError(x);
    }
    return this;
  }

  @NonNull @Override
  public StatusCode getResponseCode() {
    return StatusCode.valueOf(exchange.getStatusCode());
  }

  @NonNull @Override
  public Context setResponseCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeader(@NonNull String name) {
    exchange.getResponseHeaders().remove(name);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    exchange.getResponseHeaders().clear();
    return this;
  }

  @NonNull @Override
  public MediaType getResponseType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @NonNull @Override
  public Context setDefaultResponseType(@NonNull MediaType contentType) {
    if (responseType == null) {
      setResponseType(contentType, contentType.getCharset());
    }
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull MediaType contentType, @Nullable Charset charset) {
    this.responseType = contentType;
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    exchange.getResponseHeaders().put(CONTENT_TYPE, contentType);
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    return exchange.getResponseHeaders().getFirst(name);
  }

  @NonNull @Override
  public Context setResponseLength(long length) {
    responseLength = length;
    exchange.getResponseHeaders().put(CONTENT_LENGTH, Long.toString(length));
    return this;
  }

  @Override
  public long getResponseLength() {
    if (responseLength == -1) {
      return exchange.getResponseContentLength();
    }
    return responseLength;
  }

  @NonNull public Context setResponseCookie(@NonNull Cookie cookie) {
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

  @NonNull @Override
  public OutputStream responseStream() {
    ifStartBlocking();

    ifSetChunked();

    return exchange.getOutputStream();
  }

  @NonNull @Override
  public io.jooby.Sender responseSender() {
    return new UndertowSender(this, exchange);
  }

  @NonNull @Override
  public PrintWriter responseWriter(MediaType type, Charset charset) {
    ifStartBlocking();

    setResponseType(type, charset);
    ifSetChunked();

    return new PrintWriter(new UndertowWriter(exchange.getOutputStream(), charset));
  }

  @NonNull @Override
  public Context send(@NonNull byte[] data) {
    return send(ByteBuffer.wrap(data));
  }

  @NonNull @Override
  public Context send(@NonNull ReadableByteChannel channel) {
    ifSetChunked();
    new UndertowChunkedStream(exchange.getRequestContentLength()).send(channel, exchange, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull String data, @NonNull Charset charset) {
    return send(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    HeaderMap headers = exchange.getResponseHeaders();
    if (!headers.contains(CONTENT_LENGTH)) {
      long len = 0;
      for (ByteBuffer b : data) {
        len += b.remaining();
      }
      headers.put(Headers.CONTENT_LENGTH, Long.toString(len));
    }

    exchange.getResponseSender().send(data, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer data) {
    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, Long.toString(data.remaining()));
    exchange.getResponseSender().send(data, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull Output output) {
    output.send(this);
    return this;
  }

  @NonNull @Override
  public Context send(StatusCode statusCode) {
    exchange.setStatusCode(statusCode.value());
    exchange.getResponseSender().send(EMPTY, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return send(((FileInputStream) in).getChannel());
    }
    try {
      ifSetChunked();
      long len = exchange.getResponseContentLength();
      ByteRange range =
          ByteRange.parse(exchange.getRequestHeaders().getFirst(RANGE), len).apply(this);
      new UndertowChunkedStream(len).send(Channels.newChannel(range.apply(in)), exchange, this);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Context send(@NonNull FileChannel file) {
    try {
      long len = file.size();
      exchange.setResponseContentLength(len);
      ByteRange range =
          ByteRange.parse(exchange.getRequestHeaders().getFirst(RANGE), len).apply(this);
      file.position(range.getStart());
      new UndertowChunkedStream(range.getEnd()).send(file, exchange, this);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public boolean isResponseStarted() {
    return exchange.isResponseStarted();
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().contains(RouterOption.RESET_HEADERS_ON_ERROR)
        : resetHeadersOnError.booleanValue();
  }

  @Override
  public Context setResetHeadersOnError(boolean value) {
    this.resetHeadersOnError = value;
    return this;
  }

  @Override
  public void onComplete(HttpServerExchange exchange, Sender sender) {
    ifSaveSession();
    sender.close(IoCallback.END_EXCHANGE);
  }

  @NonNull @Override
  public Context onComplete(@NonNull Route.Complete task) {
    if (completionListener == null) {
      completionListener = new UndertowCompletionListener(this);
      exchange.addExchangeCompleteListener(completionListener);
    }
    completionListener.addListener(task);
    return this;
  }

  @Override
  public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
    destroy(exception);
  }

  @Override
  public String toString() {
    return getMethod() + " " + getRequestPath();
  }

  private void ifSaveSession() {
    if (attributes != null) {
      Session session = (Session) attributes.get(Session.NAME);
      if (session != null && (session.isNew() || session.isModify())) {
        SessionStore store = router.getSessionStore();
        store.saveSession(this, session);
      }
    }
  }

  void destroy(Exception cause) {
    try {
      if (cause != null) {
        Logger log = router.getLog();
        if (Server.connectionLost(cause)) {
          log.debug(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), cause);
        } else {
          log.error(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), cause);
        }
      }
    } finally {
      this.exchange.endExchange();
    }
  }

  private void formData(Formdata form, FormData data) {
    if (data != null) {
      for (var path : data) {
        var values = data.get(path);
        for (var value : values) {
          /*
           * BigField: true if size of the FormValue comes from a multipart request exceeds the fieldSizeThreshold of
           * {@link MultiPartParserDefinition} without filename specified.
           * See https://github.com/jooby-project/jooby/discussions/3464
           */
          if (value.isFileItem() && !value.isBigField()) {
            form.put(path, new UndertowFileUpload(path, value));
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
