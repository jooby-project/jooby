/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static io.jooby.internal.jetty.JettyCallbacks.fromByteBufferArray;
import static org.eclipse.jetty.http.HttpHeader.*;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static org.eclipse.jetty.http.HttpHeader.SET_COOKIE;
import static org.eclipse.jetty.io.Content.Sink.asOutputStream;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.content.InputStreamContentSource;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;
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
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Sender;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.WebSocket;
import io.jooby.buffer.BufferedOutput;
import io.jooby.value.Value;

public class JettyContext implements DefaultContext, Callback {
  private final int bufferSize;
  private final long maxRequestSize;
  Request request;
  Response response;

  private QueryString query;
  private Formdata formdata;
  private List<FileUpload> files;
  private Value headers;
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
  private Callback callback;
  private boolean inEventLoop;

  public JettyContext(
      InvocationType invocationType,
      Request request,
      Response response,
      Callback callback,
      Router router,
      int bufferSize,
      long maxRequestSize) {
    this.request = request;
    this.response = response;
    this.router = router;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.method = request.getMethod().toUpperCase();
    this.requestPath = request.getHttpURI().getPath();
    this.callback = callback;
    this.inEventLoop = invocationType == InvocationType.NON_BLOCKING;
  }

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public @NonNull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      this.cookies = Collections.emptyMap();
      var cookies = Request.getCookies(request);
      if (cookies != null) {
        this.cookies = new LinkedHashMap<>(cookies.size());
        for (var it : cookies) {
          this.cookies.put(it.getName(), it.getValue());
        }
      }
    }
    return cookies;
  }

  @NonNull @Override
  public Body body() {
    InputStream in = Content.Source.asInputStream(request);
    long len = request.getLength();
    if (maxRequestSize > 0) {
      in = new LimitedInputStream(in, maxRequestSize);
    }
    return Body.of(this, in, len);
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
      query = QueryString.create(getRouter().getValueFactory(), request.getHttpURI().getQuery());
    }
    return query;
  }

  @NonNull @Override
  public Formdata form() {
    if (formdata == null) {
      formdata = Formdata.create(getRouter().getValueFactory());

      formParam(request, formdata);

      // Files:
      String contentType = request.getHeaders().get(HttpHeader.CONTENT_TYPE);
      if (contentType != null
          && MimeTypes.Type.MULTIPART_FORM_DATA.is(
              HttpField.getValueParameters(contentType, null))) {
        // Is a multipart... make sure isn't empty
        String boundary = MultiPart.extractBoundary(contentType);
        if (boundary != null) {
          try {
            // Create and configure the multipart parser.
            var parser = new MultiPartFormData.Parser(boundary);
            // By default, uploaded files are stored in this directory, to
            // avoid to read the file content (which can be large) in memory.
            parser.setFilesDirectory(getRouter().getTmpdir());
            parser.setMaxMemoryFileSize(bufferSize);
            parser.setMaxLength(maxRequestSize);
            // Convert the request content into parts.
            var parts = parser.parse(request).get();
            for (var part : parts) {
              if (part.getFileName() != null) {
                String name = part.getName();
                formdata.put(name, register(new JettyFileUpload(router.getTmpdir(), part)));
              } else {
                formdata.put(part.getName(), Content.Source.asString(part.getContentSource()));
              }
            }
          } catch (Exception x) {
            throw SneakyThrows.propagate(x);
          }
        }
      }
    }
    return formdata;
  }

  @NonNull @Override
  public Value header(@NonNull String name) {
    return Value.create(
        getRouter().getValueFactory(), name, request.getHeaders().getValuesList(name));
  }

  @NonNull @Override
  public Value header() {
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      for (HttpField header : request.getHeaders()) {
        headerMap.put(header.getName(), header.getValueList());
      }
      headers = Value.headers(getRouter().getValueFactory(), headerMap);
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
      remoteAddress = Optional.ofNullable(Request.getRemoteAddr(request)).orElse("").trim();
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
    return request.getConnectionMetaData().getProtocol();
  }

  @NonNull @Override
  public List<Certificate> getClientCertificates() {
    var clientCertificates = request.getAttribute("org.eclipse.jetty.server.peerCertificates");
    return clientCertificates == null ? List.of() : List.of((Certificate[]) clientCertificates);
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
    return inEventLoop;
  }

  @NonNull @Override
  public Context dispatch(@NonNull Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @NonNull @Override
  public Context dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    if (inEventLoop) {
      inEventLoop = false;
      executor.execute(action);
    } else {
      action.run();
    }
    return this;
  }

  @NonNull @Override
  public Context detach(@NonNull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull WebSocket.Initializer handler) {
    try {
      responseStarted = true;
      request.setAttribute(JettyContext.class.getName(), this);
      var container = ServerWebSocketContainer.get(request.getContext());

      var ws = new JettyWebSocket(this);
      handler.init(Context.readOnly(this), ws);
      container.upgrade((rq, rs, cb) -> ws, request, response, callback);
      return this;
    } catch (Throwable x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
    try {
      responseStarted = true;
      handler.handle(new JettyServerSentEmitter(this, response));
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
    response.getHeaders().put(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    response.getHeaders().put(CONTENT_TYPE, contentType);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    response.getHeaders().put(name, value);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeader(@NonNull String name) {
    response.getHeaders().remove(name);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    response.reset();
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    return response.getHeaders().get(name);
  }

  @NonNull @Override
  public Context setResponseLength(long length) {
    response.getHeaders().put(CONTENT_LENGTH, length);
    return this;
  }

  @Override
  public long getResponseLength() {
    return response.getHeaders().getLongField(CONTENT_LENGTH);
  }

  @NonNull public Context setResponseCookie(@NonNull Cookie cookie) {
    if (responseCookies == null) {
      responseCookies = new HashMap<>();
    }
    cookie.setPath(cookie.getPath(getContextPath()));
    responseCookies.put(cookie.getName(), cookie.toCookieString());
    response.getHeaders().remove(SET_COOKIE);
    for (String cookieString : responseCookies.values()) {
      response.getHeaders().add(SET_COOKIE.asString(), cookieString);
    }
    return this;
  }

  @NonNull @Override
  public Sender responseSender() {
    responseStarted = true;
    ifSetChunked();
    return new JettySender(this, response);
  }

  @NonNull @Override
  public OutputStream responseStream() {
    responseStarted = true;
    ifSetChunked();
    return new JettyOutputStream(asOutputStream(response), this);
  }

  @NonNull @Override
  public PrintWriter responseWriter(MediaType type, Charset charset) {
    setResponseType(type, charset);
    return new PrintWriter(responseStream());
  }

  @NonNull @Override
  public Context send(StatusCode statusCode) {
    responseStarted = true;
    response.setStatus(statusCode.value());
    response.write(true, null, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    var length = response.getHeaders().getLongField(CONTENT_LENGTH);
    if (length <= 0) {
      setResponseLength(BufferUtil.remaining(data));
    }
    responseStarted = true;
    fromByteBufferArray(response, this, data).send();
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
  public Context send(@NonNull BufferedOutput output) {
    output.send(this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer data) {
    var length = response.getHeaders().getLongField(CONTENT_LENGTH);
    if (length <= 0) {
      setResponseLength(BufferUtil.remaining(data));
    }
    responseStarted = true;
    response.write(true, data, this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ReadableByteChannel channel) {
    ifSetChunked();
    return sendStreamInternal(Channels.newInputStream(channel));
  }

  @NonNull @Override
  public Context send(@NonNull InputStream in) {
    try {
      if (in instanceof FileInputStream) {
        setResponseLength(((FileInputStream) in).getChannel().size());
      }
      return sendStreamInternal(in);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private Context sendStreamInternal(@NonNull InputStream in) {
    try {
      var len = response.getHeaders().getLongField(CONTENT_LENGTH);
      InputStream stream;
      if (len > 0) {
        stream =
            ByteRange.parse(request.getHeaders().get(HttpHeader.RANGE), len).apply(this).apply(in);
      } else {
        response.getHeaders().put(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.asString());
        stream = in;
      }
      responseStarted = true;
      Content.copy(new InputStreamContentSource(stream), response, this);
      return this;
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @NonNull @Override
  public Context send(@NonNull FileChannel file) {
    try {
      response.getHeaders().put(CONTENT_LENGTH, file.size());
      return sendStreamInternal(Channels.newInputStream(file));
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public boolean isResponseStarted() {
    return responseStarted || response.isCommitted();
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().isResetHeadersOnError()
        : resetHeadersOnError;
  }

  @Override
  public @NonNull Context setResetHeadersOnError(boolean resetHeadersOnError) {
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

  @Override
  public void failed(Throwable x) {
    try {
      if (!isResponseStarted()) {
        sendError(x);
      }
    } finally {
      try {
        responseDone();
      } catch (Throwable ignored) {
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
        callback.failed(x);
      }
    }
  }

  @Override
  public void succeeded() {
    try {
      responseDone();
    } finally {
      callback.succeeded();
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
    }
  }

  private void ifSaveSession() {
    Session session = (Session) getAttributes().get(Session.NAME);
    if (session != null && (session.isNew() || session.isModify())) {
      SessionStore store = router.getSessionStore();
      store.saveSession(this, session);
    }
  }

  private void ifSetChunked() {
    var len = response.getHeaders().getLongField(CONTENT_LENGTH);
    if (len <= 0) {
      response.getHeaders().put(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED);
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
    try {
      var params = Request.getParameters(request);
      for (Fields.Field param : params) {
        String name = param.getName();
        var values = param.getValues();
        if (values != null) {
          for (String value : values) {
            form.put(name, value);
          }
        }
      }
    } catch (Exception ex) {
      throw SneakyThrows.propagate(ex);
    }
  }
}
