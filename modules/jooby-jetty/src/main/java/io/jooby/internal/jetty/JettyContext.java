/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static org.eclipse.jetty.http.HttpHeader.SET_COOKIE;
import static org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT;

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
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.MultiPartFormInputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.ByteRange;
import io.jooby.CompletionListeners;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOption;
import io.jooby.Sender;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.Value;
import io.jooby.ValueNode;
import io.jooby.WebSocket;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Part;

public class JettyContext implements DefaultContext {
  private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);
  private final int bufferSize;
  private final long maxRequestSize;
  Request request;
  Response response;

  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<FileUpload> files;
  private ValueNode headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> attributes = new HashMap<>();
  private Router router;
  private Route route;
  private MediaType responseType;
  private Map<String, String> cookies;
  private HashMap<String, String> responseCookies;
  private boolean responseStarted;
  private Boolean resetHeadersOnError;
  private String method;
  private String requestPath;
  private CompletionListeners listeners;
  private String remoteAddress;
  private String host;
  private String scheme;
  private int port;

  public JettyContext(Request request, Router router, int bufferSize, long maxRequestSize) {
    this.request = request;
    this.response = request.getResponse();
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.method = request.getMethod().toUpperCase();
    this.requestPath = request.getRequestURI();
  }

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public @NonNull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      this.cookies = Collections.emptyMap();
      jakarta.servlet.http.Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        this.cookies = new LinkedHashMap<>(cookies.length);
        for (jakarta.servlet.http.Cookie it : cookies) {
          this.cookies.put(it.getName(), it.getValue());
        }
      }
    }
    return cookies;
  }

  @NonNull @Override
  public Body body() {
    try {
      InputStream in = request.getInputStream();
      long len = request.getContentLengthLong();
      if (maxRequestSize > 0) {
        in = new LimitedInputStream(in, maxRequestSize);
      }
      return Body.of(this, in, len);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Router getRouter() {
    return router;
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

  @NonNull @Override
  public QueryString query() {
    if (query == null) {
      query = QueryString.create(this, request.getQueryString());
    }
    return query;
  }

  @NonNull @Override
  public Formdata form() {
    if (form == null) {
      form = Formdata.create(this);
      formParam(request, form);
    }
    return form;
  }

  @NonNull @Override
  public Multipart multipart() {
    if (multipart == null) {
      multipart = Multipart.create(this);
      form = multipart;

      request.setAttribute(
          __MULTIPART_CONFIG_ELEMENT,
          new MultipartConfigElement(
              router.getTmpdir().toString(), -1L, maxRequestSize, bufferSize));

      formParam(request, multipart);

      // Files:
      String contentType = request.getContentType();
      if (contentType != null
          && MimeTypes.Type.MULTIPART_FORM_DATA.is(HttpField.valueParameters(contentType, null))) {
        try {
          Collection<Part> parts = request.getParts();
          for (Part part : parts) {
            if (part.getSubmittedFileName() != null) {
              String name = part.getName();
              multipart.put(
                  name, register(new JettyFileUpload((MultiPartFormInputStream.MultiPart) part)));
            }
          }
        } catch (IOException | ServletException x) {
          throw SneakyThrows.propagate(x);
        }
      }
    }
    return multipart;
  }

  @NonNull @Override
  public ValueNode header() {
    if (headers == null) {
      Enumeration<String> names = request.getHeaderNames();
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        headerMap.put(name, Collections.list(request.getHeaders(name)));
      }
      headers = Value.headers(this, headerMap);
    }
    return headers;
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
  public String getRemoteAddress() {
    if (remoteAddress == null) {
      String remoteAddr = Optional.ofNullable(request.getRemoteAddr()).orElse("").trim();
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
    return request.getProtocol();
  }

  @NonNull @Override
  public List<Certificate> getClientCertificates() {
    return Arrays.asList(
        (Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate"));
  }

  @NonNull @Override
  public String getScheme() {
    if (scheme == null) {
      scheme = request.isSecure() ? "https" : "http";
    }
    return scheme;
  }

  @NonNull @Override
  public Context setScheme(@NonNull String scheme) {
    this.scheme = scheme;
    return this;
  }

  @Override
  public boolean isInIoThread() {
    return false;
  }

  @NonNull @Override
  public Context dispatch(@NonNull Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @NonNull @Override
  public Context dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    if (router.getWorker() == executor) {
      action.run();
    } else {
      ifStartAsync();
      executor.execute(action);
    }
    return this;
  }

  @NonNull @Override
  public Context detach(@NonNull Route.Handler next) throws Exception {
    ifStartAsync();
    next.apply(this);
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull WebSocket.Initializer handler) {
    try {
      responseStarted = true;
      request.setAttribute(JettyContext.class.getName(), this);
      JettyWebSocketServerContainer container =
          JettyWebSocketServerContainer.getContainer(request.getServletContext());

      JettyWebSocket ws = new JettyWebSocket(this);
      JettyWebSocketCreator creator = (upgradeRequest, upgradeResponse) -> ws;
      handler.init(Context.readOnly(this), ws);
      container.upgrade(creator, request, response);
      return this;
    } catch (Throwable x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
    try {
      responseStarted = true;
      AsyncContext async =
          request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();
      /** Infinite timeout because the continuation is never resumed but only completed on close. */
      async.setTimeout(0L);

      response.flushBuffer();

      handler.handle(new JettyServerSentEmitter(this));

      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public StatusCode getResponseCode() {
    return StatusCode.valueOf(response.getStatus());
  }

  @NonNull @Override
  public Context setResponseCode(int statusCode) {
    response.setStatus(statusCode);
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
    response.setHeader(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    response.setHeader(CONTENT_TYPE, contentType);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    response.setHeader(name, value);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeader(@NonNull String name) {
    response.setHeader(name, null);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    response.reset();
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    return response.getHeader(name);
  }

  @NonNull @Override
  public Context setResponseLength(long length) {
    response.setContentLengthLong(length);
    return this;
  }

  @Override
  public long getResponseLength() {
    long responseLength = response.getContentLength();
    if (responseLength == -1) {
      String lenStr = response.getHeader(HttpHeader.CONTENT_LENGTH.asString());
      if (lenStr != null) {
        responseLength = Long.parseLong(lenStr);
      }
    }
    return responseLength;
  }

  @NonNull public Context setResponseCookie(@NonNull Cookie cookie) {
    if (responseCookies == null) {
      responseCookies = new HashMap<>();
    }
    cookie.setPath(cookie.getPath(getContextPath()));
    responseCookies.put(cookie.getName(), cookie.toCookieString());
    response.setHeader(SET_COOKIE, null);
    for (String cookieString : responseCookies.values()) {
      response.addHeader(SET_COOKIE.asString(), cookieString);
    }
    return this;
  }

  @NonNull @Override
  public Sender responseSender() {
    responseStarted = true;
    ifSetChunked();
    ifStartAsync();
    return new JettySender(this, response.getHttpOutput());
  }

  @NonNull @Override
  public OutputStream responseStream() {
    responseStarted = true;
    try {
      ifSetChunked();
      return new JettyOutputStream(response.getOutputStream(), this);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public PrintWriter responseWriter(MediaType type, Charset charset) {
    setResponseType(type, charset);
    return new PrintWriter(responseStream());
  }

  @NonNull @Override
  public Context send(StatusCode statusCode) {
    response.setStatus(statusCode.value());
    send(EMPTY_BUFFER);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    if (response.getContentLength() <= 0) {
      setResponseLength(BufferUtil.remaining(data));
    }
    ifStartAsync();
    HttpOutput out = response.getHttpOutput();
    out.setWriteListener(writeListener(request.getAsyncContext(), out, data));
    responseStarted = true;
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull byte[] data) {
    return send(ByteBuffer.wrap(data));
  }

  @NonNull @Override
  public Context send(@NonNull String data, @NonNull Charset charset) {
    return send(ByteBuffer.wrap(data.getBytes(charset)));
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer data) {
    try {
      if (response.getContentLength() == -1) {
        response.setContentLengthLong(data.remaining());
      }
      responseStarted = true;
      HttpOutput sender = response.getHttpOutput();
      sender.sendContent(data);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    } finally {
      responseDone();
    }
  }

  @NonNull @Override
  public Context send(@NonNull ReadableByteChannel channel) {
    responseStarted = true;
    ifSetChunked();
    return sendStreamInternal(Channels.newInputStream(channel));
  }

  @NonNull @Override
  public Context send(@NonNull InputStream in) {
    try {
      if (in instanceof FileInputStream) {
        response.setLongContentLength(((FileInputStream) in).getChannel().size());
      }
      return sendStreamInternal(in);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private Context sendStreamInternal(@NonNull InputStream in) {
    try {
      long len = response.getContentLength();
      InputStream stream;
      if (len > 0) {
        stream =
            ByteRange.parse(request.getHeader(HttpHeader.RANGE.asString()), len)
                .apply(this)
                .apply(in);
      } else {
        response.setHeader(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.asString());
        stream = in;
      }
      responseStarted = true;
      response.getHttpOutput().sendContent(stream);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    } finally {
      responseDone();
    }
  }

  @NonNull @Override
  public Context send(@NonNull FileChannel file) {
    try (FileChannel channel = file) {
      response.setLongContentLength(channel.size());
      return sendStreamInternal(Channels.newInputStream(file));
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    } finally {
      responseDone();
    }
  }

  @Override
  public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().contains(RouterOption.RESET_HEADERS_ON_ERROR)
        : resetHeadersOnError.booleanValue();
  }

  @Override
  public Context setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }

  @NonNull @Override
  public Context onComplete(@NonNull Route.Complete task) {
    if (listeners == null) {
      listeners = new CompletionListeners();
    }
    listeners.addListener(task);
    return this;
  }

  @Override
  public String toString() {
    return getMethod() + " " + getRequestPath();
  }

  void complete(Throwable x) {
    try {
      Logger log = router.getLog();
      if (x != null) {
        if (Server.connectionLost(x)) {
          log.debug(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), x);
        } else {
          log.error(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), x);
        }
      }
    } finally {
      responseDone();
    }
  }

  private void clearFiles() {
    if (files != null) {
      for (FileUpload file : files) {
        try {
          file.close();
        } catch (Exception e) {
          router.getLog().debug("file upload destroy resulted in exception", e);
        }
      }
      files.clear();
      files = null;
    }
  }

  void responseDone() {
    try {
      ifSaveSession();

      clearFiles();
    } finally {
      if (listeners != null) {
        listeners.run(this);
      }
      if (request.isAsyncStarted()) {
        request.getAsyncContext().complete();
      }
    }
  }

  private void ifSaveSession() {
    Session session = (Session) getAttributes().get(Session.NAME);
    if (session != null && (session.isNew() || session.isModify())) {
      SessionStore store = router.getSessionStore();
      store.saveSession(this, session);
    }
  }

  private void ifStartAsync() {
    if (!request.isAsyncStarted()) {
      request.startAsync();
    }
  }

  private void ifSetChunked() {
    if (response.getContentLength() <= 0) {
      response.setHeader(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.asString());
    }
  }

  private FileUpload register(FileUpload upload) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(upload);
    return upload;
  }

  private static void formParam(Request request, Formdata form) {
    Enumeration<String> names = request.getParameterNames();
    MultiMap<String> query = request.getQueryParameters();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (query == null || !query.containsKey(name)) {
        String[] values = request.getParameterValues(name);
        if (values != null) {
          for (String value : values) {
            form.put(name, value);
          }
        }
      }
    }
  }

  private static WriteListener writeListener(
      AsyncContext async, HttpOutput out, ByteBuffer[] data) {
    return new WriteListener() {
      int i = 0;

      @Override
      public void onWritePossible() throws IOException {
        while (out.isReady()) {
          if (i < data.length) {
            out.write(data[i++]);
          } else {
            async.complete();
            return;
          }
        }
      }

      @Override
      public void onError(Throwable x) {
        async.complete();
      }
    };
  }
}
