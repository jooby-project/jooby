/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.CompletionListeners;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.FileDownload;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Sender;
import io.jooby.ServerOptions;
import io.jooby.ServerSentEmitter;
import io.jooby.Session;
import io.jooby.StatusCode;
import io.jooby.Value;
import io.jooby.WebSocket;
import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DataBufferFactory;
import io.jooby.buffer.DefaultDataBufferFactory;
import io.jooby.exception.TypeMismatchException;
import io.jooby.value.ValueFactory;

/** Unit test friendly context implementation. Allows to set context properties. */
public class MockContext implements DefaultContext {

  private String method = Router.GET;

  private Route route;

  private String requestPath = "/";

  private Map<String, String> pathMap = new HashMap<>();

  private String queryString;

  private Map<String, Collection<String>> headers = new HashMap<>();

  private ValueFactory valueFactory = new ValueFactory();

  private Formdata formdata = Formdata.create(valueFactory);

  private Body body;

  private Object bodyObject;

  private Map<String, MessageDecoder> decoders = new HashMap<>();

  private Map<String, Object> responseHeaders = new HashMap<>();

  private Map<String, Object> attributes = new HashMap<>();

  private MockResponse response = new MockResponse();

  private Map<String, String> cookies = new LinkedHashMap<>();

  private FlashMap flashMap = FlashMap.create(this, new Cookie("jooby.sid").setHttpOnly(true));

  private Session session;

  private Router router;

  private Map<String, List<FileUpload>> files = new LinkedHashMap<>();

  private boolean responseStarted;

  private boolean resetHeadersOnError = true;

  private CompletionListeners listeners = new CompletionListeners();

  private Consumer<MockResponse> consumer;

  private MockRouter mockRouter;

  private String remoteAddress = "0.0.0.0";

  private String host;

  private String scheme = "http";

  private int port = -1;

  private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

  @NonNull @Override
  public String getMethod() {
    return method;
  }

  @NonNull @Override
  public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @Override
  public int getPort() {
    return port;
  }

  @NonNull @Override
  public DataBufferFactory getBufferFactory() {
    return bufferFactory;
  }

  /**
   * Set HTTP method.
   *
   * @param method HTTP method.
   * @return This context.
   */
  @Override
  public @NonNull MockContext setMethod(@NonNull String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @Override
  public @NonNull Session session() {
    if (session == null) {
      session = new MockSession(this);
    }
    return session;
  }

  /**
   * Set mock session.
   *
   * @param session Mock session.
   * @return This context.
   */
  public @NonNull MockContext setSession(@NonNull MockSession session) {
    this.session = session;
    return this;
  }

  @Nullable @Override
  public Session sessionOrNull() {
    return session;
  }

  @NonNull @Override
  public Map<String, String> cookieMap() {
    return cookies;
  }

  @NonNull @Override
  public Object forward(@NonNull String path) {
    setRequestPath(path);
    if (mockRouter != null) {
      return mockRouter.call(getMethod(), path, this, consumer).value();
    }
    return this;
  }

  /**
   * Set cookie map.
   *
   * @param cookies Cookie map.
   * @return This context.
   */
  @NonNull public MockContext setCookieMap(@NonNull Map<String, String> cookies) {
    this.cookies = cookies;
    return this;
  }

  @NonNull @Override
  public FlashMap flash() {
    return flashMap;
  }

  /**
   * Set flash map.
   *
   * @param flashMap Flash map.
   * @return This context.
   */
  public MockContext setFlashMap(@NonNull FlashMap flashMap) {
    this.flashMap = flashMap;
    return this;
  }

  /**
   * Set request flash attribute.
   *
   * @param name Flash name.
   * @param value Flash value.
   * @return This context.
   */
  @NonNull public MockContext setFlashAttribute(@NonNull String name, @NonNull String value) {
    flashMap.put(name, value);
    return this;
  }

  @NonNull @Override
  public Route getRoute() {
    return route;
  }

  @NonNull @Override
  public MockContext setRoute(@NonNull Route route) {
    this.route = route;
    return this;
  }

  @NonNull @Override
  public String getRequestPath() {
    return requestPath;
  }

  /**
   * Set requestPath.
   *
   * @param pathString Path string.
   * @return This context.
   */
  @Override
  public @NonNull MockContext setRequestPath(@NonNull String pathString) {
    int q = pathString.indexOf("?");
    if (q > 0) {
      this.requestPath = pathString.substring(0, q);
    } else {
      this.requestPath = pathString;
    }
    return this;
  }

  @NonNull @Override
  public Map<String, String> pathMap() {
    return pathMap;
  }

  @NonNull @Override
  public MockContext setPathMap(@NonNull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @NonNull @Override
  public QueryString query() {
    return QueryString.create(valueFactory, queryString);
  }

  @NonNull @Override
  public String queryString() {
    return queryString;
  }

  /**
   * Set query string value.
   *
   * @param queryString Query string (starting with <code>?</code>).
   * @return This context.
   */
  public @NonNull MockContext setQueryString(@NonNull String queryString) {
    this.queryString = queryString;
    return this;
  }

  @NonNull @Override
  public Value header() {
    return Value.headers(valueFactory, headers);
  }

  /**
   * Set request headers.
   *
   * @param headers Request headers.
   * @return This context.
   */
  @NonNull public MockContext setHeaders(@NonNull Map<String, Collection<String>> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Set request headers.
   *
   * @param name Request header.
   * @param value Request value.
   * @return This context.
   */
  @NonNull public MockContext setRequestHeader(@NonNull String name, @NonNull String value) {
    Collection<String> values = this.headers.computeIfAbsent(name, k -> new ArrayList<>());
    values.add(value);
    return this;
  }

  @NonNull @Override
  public Formdata form() {
    return formdata;
  }

  @NonNull @Override
  public List<FileUpload> files() {
    return files.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  /**
   * Set mock files.
   *
   * @param name HTTP name.
   * @param file Mock files.
   * @return This context.
   */
  public MockContext setFile(@NonNull String name, @NonNull FileUpload file) {
    this.files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
    return this;
  }

  @NonNull @Override
  public List<FileUpload> files(@NonNull String name) {
    return files.entrySet().stream()
        .filter(it -> it.getKey().equals(name))
        .flatMap(it -> it.getValue().stream())
        .collect(Collectors.toList());
  }

  @NonNull @Override
  public FileUpload file(@NonNull String name) {
    return files.entrySet().stream()
        .filter(it -> it.getKey().equals(name))
        .findFirst()
        .map(it -> it.getValue().get(0))
        .orElseThrow(() -> new TypeMismatchException(name, FileUpload.class));
  }

  /**
   * Set form data.
   *
   * @param formdata Form.
   * @return This context.
   */
  @NonNull public MockContext setForm(@NonNull Formdata formdata) {
    this.formdata = formdata;
    return this;
  }

  @NonNull @Override
  public Body body() {
    if (body == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    return body;
  }

  @NonNull @Override
  public <T> T body(@NonNull Class<T> type) {
    return decode(type, MediaType.text);
  }

  @NonNull @Override
  public <T> T body(@NonNull Type type) {
    return decode(type, MediaType.text);
  }

  @NonNull @Override
  public <T> T decode(@NonNull Type type, @NonNull MediaType contentType) {
    if (bodyObject == null) {
      throw new IllegalStateException("No body was set, use setBodyObject() to set one.");
    }
    Reified<?> reified = Reified.get(type);
    if (!reified.getRawType().isInstance(bodyObject)) {
      throw new TypeMismatchException("body", type);
    }
    return (T) bodyObject;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @NonNull public MockContext setBody(@NonNull Body body) {
    this.body = body;
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @NonNull public MockContext setBodyObject(@NonNull Object body) {
    this.bodyObject = body;
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @NonNull public MockContext setBody(@NonNull String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return setBody(bytes);
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @NonNull public MockContext setBody(@NonNull byte[] body) {
    setBody(Body.of(this, new ByteArrayInputStream(body), body.length));
    return this;
  }

  @NonNull @Override
  public MessageDecoder decoder(@NonNull MediaType contentType) {
    return decoders.getOrDefault(contentType, MessageDecoder.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override
  public boolean isInIoThread() {
    return false;
  }

  @NonNull @Override
  public MockContext dispatch(@NonNull Runnable action) {
    action.run();
    return this;
  }

  @NonNull @Override
  public MockContext dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    action.run();
    return this;
  }

  @NonNull @Override
  public MockContext detach(@NonNull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @NonNull @Override
  public MockContext removeResponseHeader(@NonNull String name) {
    responseHeaders.remove(name);
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    Object value = responseHeaders.get(name);
    return value == null ? null : value.toString();
  }

  @NonNull @Override
  public MockContext setResponseHeader(@NonNull String name, @NonNull String value) {
    responseHeaders.put(name, value);
    return this;
  }

  @NonNull @Override
  public MockContext setResponseLength(long length) {
    response.setContentLength(length);
    return this;
  }

  @Override
  public long getResponseLength() {
    return response.getContentLength();
  }

  @NonNull @Override
  public MockContext setResponseType(@NonNull String contentType) {
    response.setContentType(MediaType.valueOf(contentType));
    return this;
  }

  @NonNull @Override
  public MockContext setResponseType(@NonNull MediaType contentType, @Nullable Charset charset) {
    response.setContentType(contentType);
    return this;
  }

  @NonNull @Override
  public MockContext setResponseCode(int statusCode) {
    response.setStatusCode(StatusCode.valueOf(statusCode));
    return this;
  }

  @NonNull @Override
  public StatusCode getResponseCode() {
    return response.getStatusCode();
  }

  @NonNull @Override
  public MockContext render(@NonNull Object result) {
    responseStarted = true;
    this.response.setResult(result);
    return this;
  }

  /**
   * Mock response generated from route execution.
   *
   * @return Mock response.
   */
  @NonNull public MockResponse getResponse() {
    response.setHeaders(responseHeaders);
    return response;
  }

  @NonNull @Override
  public OutputStream responseStream() {
    responseStarted = true;
    ByteArrayOutputStream out = new ByteArrayOutputStream(ServerOptions._16KB);
    this.response.setResult(out);
    return out;
  }

  @NonNull @Override
  public Sender responseSender() {
    responseStarted = true;
    return new Sender() {
      @Override
      public Sender write(@NonNull byte[] data, @NonNull Callback callback) {
        response.setResult(data);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @NonNull @Override
      public Sender write(@NonNull DataBuffer data, @NonNull Callback callback) {
        response.setResult(data);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @Override
      public void close() {
        listeners.run(MockContext.this);
      }
    };
  }

  @NonNull @Override
  public String getHost() {
    return host;
  }

  @NonNull @Override
  public Context setHost(@NonNull String host) {
    this.host = host;
    return this;
  }

  @NonNull @Override
  public String getRemoteAddress() {
    return remoteAddress;
  }

  @NonNull @Override
  public Context setRemoteAddress(@NonNull String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @NonNull @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @NonNull @Override
  public List<Certificate> getClientCertificates() {
    return new ArrayList<Certificate>();
  }

  @NonNull @Override
  public String getScheme() {
    return scheme;
  }

  @NonNull @Override
  public Context setScheme(@NonNull String scheme) {
    this.scheme = scheme;
    return this;
  }

  @NonNull @Override
  public PrintWriter responseWriter(MediaType type, Charset charset) {
    responseStarted = true;
    PrintWriter writer = new PrintWriter(new StringWriter());
    this.response.setResult(writer).setContentType(type);
    return writer;
  }

  @NonNull @Override
  public MockContext send(@NonNull String data, @NonNull Charset charset) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.length());
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(@NonNull byte[] data) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.length);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(@NonNull byte[]... data) {
    responseStarted = true;
    this.response
        .setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].length).sum());
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(@NonNull ByteBuffer data) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.remaining());
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull DataBuffer data) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.readableByteCount());
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    responseStarted = true;
    this.response
        .setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].remaining()).sum());
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(InputStream input) {
    responseStarted = true;
    this.response.setResult(input);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull FileDownload file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull Path file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(@NonNull ReadableByteChannel channel) {
    responseStarted = true;
    this.response.setResult(channel);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(@NonNull FileChannel file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext send(StatusCode statusCode) {
    responseStarted = true;
    this.response.setContentLength(0).setStatusCode(statusCode);
    return this;
  }

  @NonNull @Override
  public MockContext sendError(@NonNull Throwable cause) {
    return sendError(cause, router.errorCode(cause));
  }

  @NonNull @Override
  public MockContext sendError(@NonNull Throwable cause, @NonNull StatusCode code) {
    responseStarted = true;
    this.response.setResult(cause).setStatusCode(router.errorCode(cause));
    listeners.run(this);
    return this;
  }

  @NonNull @Override
  public MockContext setDefaultResponseType(@NonNull MediaType contentType) {
    response.setContentType(contentType);
    return this;
  }

  @NonNull @Override
  public MockContext setResponseCookie(@NonNull Cookie cookie) {
    String setCookie = (String) response.getHeaders().get("Set-Cookie");
    if (setCookie == null) {
      setCookie = cookie.toCookieString();
    } else {
      setCookie += ";" + cookie.toCookieString();
    }
    response.setHeader("Set-Cookie", setCookie);
    return this;
  }

  @NonNull @Override
  public MediaType getResponseType() {
    return response.getContentType();
  }

  @NonNull @Override
  public MockContext setResponseCode(@NonNull StatusCode statusCode) {
    response.setStatusCode(statusCode);
    return this;
  }

  @Override
  public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError;
  }

  @Override
  public MockContext setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    responseHeaders.clear();
    return this;
  }

  @NonNull @Override
  public Router getRouter() {
    return router;
  }

  /**
   * Set a mock router.
   *
   * @param router Mock router.
   * @return This context.
   */
  @NonNull public MockContext setRouter(@NonNull Router router) {
    this.router = router;
    return this;
  }

  @NonNull @Override
  public <T> T convert(@NonNull Value value, @NonNull Class<T> type) {
    return DefaultContext.super.convert(value, type);
  }

  @NonNull @Override
  public MockContext upgrade(@NonNull WebSocket.Initializer handler) {
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
    return this;
  }

  @NonNull @Override
  public Context onComplete(@NonNull Route.Complete task) {
    listeners.addListener(task);
    return this;
  }

  @Override
  public String toString() {
    return method + " " + requestPath;
  }

  void setConsumer(Consumer<MockResponse> consumer) {
    this.consumer = consumer;
  }

  void setMockRouter(MockRouter mockRouter) {
    this.mockRouter = mockRouter;
  }

  public ValueFactory getValueFactory() {
    return valueFactory;
  }

  public void setValueFactory(ValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }
}
