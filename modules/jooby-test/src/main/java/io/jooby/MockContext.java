/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.exception.TypeMismatchException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

/**
 * Unit test friendly context implementation. Allows to set context properties.
 */
public class MockContext implements DefaultContext {

  private String method = Router.GET;

  private Route route;

  private String requestPath = "/";

  private Map<String, String> pathMap = new HashMap<>();

  private String queryString;

  private Map<String, Collection<String>> headers = new HashMap<>();

  private Formdata formdata = Formdata.create(this);

  private Multipart multipart = Multipart.create(this);

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

  @Nonnull @Override public String getMethod() {
    return method;
  }

  @Nonnull @Override public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @Override public int getPort() {
    return port;
  }

  /**
   * Set HTTP method.
   *
   * @param method HTTP method.
   * @return This context.
   */
  @Override public @Nonnull MockContext setMethod(@Nonnull String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @Override public @Nonnull Session session() {
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
  public @Nonnull MockContext setSession(@Nonnull MockSession session) {
    this.session = session;
    return this;
  }

  @Nullable @Override public Session sessionOrNull() {
    return session;
  }

  @Nonnull @Override public Map<String, String> cookieMap() {
    return cookies;
  }

  @Nonnull @Override public Context forward(@Nonnull String path) {
    setRequestPath(path);
    if (mockRouter != null) {
      mockRouter.call(getMethod(), path, this, consumer);
    }
    return this;
  }

  /**
   * Set cookie map.
   *
   * @param cookies Cookie map.
   * @return This context.
   */
  @Nonnull public MockContext setCookieMap(@Nonnull Map<String, String> cookies) {
    this.cookies = cookies;
    return this;
  }

  @Nonnull @Override public FlashMap flash() {
    return flashMap;
  }

  /**
   * Set flash map.
   *
   * @param flashMap Flash map.
   * @return This context.
   */
  public MockContext setFlashMap(@Nonnull FlashMap flashMap) {
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
  @Nonnull public MockContext setFlashAttribute(@Nonnull String name, @Nonnull String value) {
    flashMap.put(name, value);
    return this;
  }

  @Nonnull @Override public Route getRoute() {
    return route;
  }

  @Nonnull @Override public MockContext setRoute(@Nonnull Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String getRequestPath() {
    return requestPath;
  }

  /**
   * Set requestPath.
   *
   * @param pathString Path string.
   * @return This context.
   */
  @Override public @Nonnull MockContext setRequestPath(@Nonnull String pathString) {
    int q = pathString.indexOf("?");
    if (q > 0) {
      this.requestPath = pathString.substring(0, q);
    } else {
      this.requestPath = pathString;
    }
    return this;
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public MockContext setPathMap(@Nonnull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Nonnull @Override public QueryString query() {
    return QueryString.create(this, queryString);
  }

  @Nonnull @Override public String queryString() {
    return queryString;
  }

  /**
   * Set query string value.
   *
   * @param queryString Query string (starting with <code>?</code>).
   * @return This context.
   */
  public @Nonnull MockContext setQueryString(@Nonnull String queryString) {
    this.queryString = queryString;
    return this;
  }

  @Nonnull @Override public ValueNode header() {
    return Value.hash(this, headers);
  }

  /**
   * Set request headers.
   *
   * @param headers Request headers.
   * @return This context.
   */
  @Nonnull public MockContext setHeaders(@Nonnull Map<String, Collection<String>> headers) {
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
  @Nonnull public MockContext setRequestHeader(@Nonnull String name, @Nonnull String value) {
    Collection<String> values = this.headers.computeIfAbsent(name, k -> new ArrayList<>());
    values.add(value);
    return this;
  }

  @Nonnull @Override public Formdata form() {
    return formdata;
  }

  /**
   * Set formdata.
   *
   * @param formdata Formdata.
   * @return This context.
   */
  @Nonnull public MockContext setForm(@Nonnull Formdata formdata) {
    this.formdata = formdata;
    return this;
  }

  @Nonnull @Override public Multipart multipart() {
    return multipart;
  }

  @Nonnull @Override public List<FileUpload> files() {
    return files.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  /**
   * Set mock files.
   *
   * @param name HTTP name.
   * @param file Mock files.
   * @return This context.
   */
  public MockContext setFile(@Nonnull String name, @Nonnull FileUpload file) {
    this.files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
    return this;
  }

  @Nonnull @Override public List<FileUpload> files(@Nonnull String name) {
    return files.entrySet().stream()
        .filter(it -> it.getKey().equals(name))
        .flatMap(it -> it.getValue().stream())
        .collect(Collectors.toList());
  }

  @Nonnull @Override public FileUpload file(@Nonnull String name) {
    return files.entrySet().stream()
        .filter(it -> it.getKey().equals(name))
        .findFirst()
        .map(it -> it.getValue().get(0))
        .orElseThrow(() -> new TypeMismatchException(name, FileUpload.class));
  }

  /**
   * Set multipart.
   *
   * @param multipart Multipart.
   * @return This context.
   */
  @Nonnull public MockContext setMultipart(@Nonnull Multipart multipart) {
    this.multipart = multipart;
    return this;
  }

  @Nonnull @Override public Body body() {
    if (body == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    return body;
  }

  @Nonnull @Override public <T> T body(@Nonnull Class<T> type) {
    return decode(type, MediaType.text);
  }

  @Nonnull @Override public <T> T body(@Nonnull Type type) {
    return decode(type, MediaType.text);
  }

  @Nonnull @Override public <T> T decode(@Nonnull Type type, @Nonnull MediaType contentType) {
    if (bodyObject == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
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
  @Nonnull public MockContext setBody(@Nonnull Object body) {
    if (body instanceof Body) {
      this.body = (Body) body;
    } else {
      this.bodyObject = body;
    }
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @Nonnull public MockContext setBody(@Nonnull String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return setBody(bytes);
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @Nonnull public MockContext setBody(@Nonnull byte[] body) {
    this.body = Body.of(this, new ByteArrayInputStream(body), body.length);
    return this;
  }

  @Nonnull @Override public MessageDecoder decoder(@Nonnull MediaType contentType) {
    return decoders.getOrDefault(contentType, MessageDecoder.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override public boolean isInIoThread() {
    return false;
  }

  @Nonnull @Override public MockContext dispatch(@Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override
  public MockContext dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override public MockContext detach(@Nonnull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Nonnull @Override public MockContext removeResponseHeader(@Nonnull String name) {
    responseHeaders.remove(name);
    return this;
  }

  @Nullable @Override public String getResponseHeader(@Nonnull String name) {
    Object value = responseHeaders.get(name);
    return value == null ? null : value.toString();
  }

  @Nonnull @Override
  public MockContext setResponseHeader(@Nonnull String name, @Nonnull String value) {
    responseHeaders.put(name, value);
    return this;
  }

  @Nonnull @Override public MockContext setResponseLength(long length) {
    response.setContentLength(length);
    return this;
  }

  @Override public long getResponseLength() {
    return response.getContentLength();
  }

  @Nonnull @Override public MockContext setResponseType(@Nonnull String contentType) {
    response.setContentType(MediaType.valueOf(contentType));
    return this;
  }

  @Nonnull @Override
  public MockContext setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    response.setContentType(contentType);
    return this;
  }

  @Nonnull @Override public MockContext setResponseCode(int statusCode) {
    response.setStatusCode(StatusCode.valueOf(statusCode));
    return this;
  }

  @Nonnull @Override public StatusCode getResponseCode() {
    return response.getStatusCode();
  }

  @Nonnull @Override public MockContext render(@Nonnull Object result) {
    responseStarted = true;
    this.response.setResult(result);
    return this;
  }

  /**
   * Mock response generated from route execution.
   *
   * @return Mock response.
   */
  @Nonnull public MockResponse getResponse() {
    responseStarted = true;
    response.setHeaders(responseHeaders);
    return response;
  }

  @Nonnull @Override public OutputStream responseStream() {
    responseStarted = true;
    ByteArrayOutputStream out = new ByteArrayOutputStream(ServerOptions._16KB);
    this.response.setResult(out);
    return out;
  }

  @Nonnull @Override public Sender responseSender() {
    responseStarted = true;
    return new Sender() {
      @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
        response.setResult(data);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @Override public void close() {
        listeners.run(MockContext.this);
      }
    };
  }

  @Nonnull @Override public String getHost() {
    return host;
  }

  @Nonnull @Override public Context setHost(@Nonnull String host) {
    this.host = host;
    return this;
  }

  @Nonnull @Override public String getRemoteAddress() {
    return remoteAddress;
  }

  @Nonnull @Override public Context setRemoteAddress(@Nonnull String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Nonnull @Override public String getProtocol() {
    return "HTTP/1.1";
  }

  @Nonnull @Override public String getScheme() {
    return scheme;
  }

  @Nonnull @Override public Context setScheme(@Nonnull String scheme) {
    this.scheme = scheme;
    return this;
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    responseStarted = true;
    PrintWriter writer = new PrintWriter(new StringWriter());
    this.response.setResult(writer)
        .setContentType(type);
    return writer;
  }

  @Nonnull @Override public MockContext send(@Nonnull String data, @Nonnull Charset charset) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.length());
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull byte[] data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.length);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull byte[]... data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].length).sum());
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull ByteBuffer data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.remaining());
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer[] data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].remaining()).sum());
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(InputStream input) {
    responseStarted = true;
    this.response.setResult(input);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull ReadableByteChannel channel) {
    responseStarted = true;
    this.response.setResult(channel);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull FileChannel file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext send(StatusCode statusCode) {
    responseStarted = true;
    this.response
        .setContentLength(0)
        .setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public MockContext sendError(@Nonnull Throwable cause) {
    return sendError(cause, router.errorCode(cause));
  }

  @Nonnull @Override
  public MockContext sendError(@Nonnull Throwable cause, @Nonnull StatusCode code) {
    responseStarted = true;
    this.response.setResult(cause)
        .setStatusCode(router.errorCode(cause));
    listeners.run(this);
    return this;
  }

  @Nonnull @Override public MockContext setDefaultResponseType(@Nonnull MediaType contentType) {
    response.setContentType(contentType);
    return this;
  }

  @Nonnull @Override public MockContext setResponseCookie(@Nonnull Cookie cookie) {
    String setCookie = (String) response.getHeaders().get("Set-Cookie");
    if (setCookie == null) {
      setCookie = cookie.toCookieString();
    } else {
      setCookie += ";" + cookie.toCookieString();
    }
    response.setHeader("Set-Cookie", setCookie);
    return this;
  }

  @Nonnull @Override public MediaType getResponseType() {
    return response.getContentType();
  }

  @Nonnull @Override public MockContext setResponseCode(@Nonnull StatusCode statusCode) {
    response.setStatusCode(statusCode);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override public boolean getResetHeadersOnError() {
    return resetHeadersOnError;
  }

  @Override public MockContext setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }

  @Nonnull @Override public Context removeResponseHeaders() {
    responseHeaders.clear();
    return this;
  }

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  /**
   * Set a mock router.
   *
   * @param router Mock router.
   * @return This context.
   */
  @Nonnull public MockContext setRouter(@Nonnull Router router) {
    this.router = router;
    return this;
  }

  @Nullable @Override public <T> T convert(ValueNode value, Class<T> type) {
    return DefaultContext.super.convert(value, type);
  }

  @Nonnull @Override public MockContext upgrade(@Nonnull WebSocket.Initializer handler) {
    return this;
  }

  @Nonnull @Override public Context upgrade(@Nonnull ServerSentEmitter.Handler handler) {
    return this;
  }

  @Nonnull @Override public Context onComplete(@Nonnull Route.Complete task) {
    listeners.addListener(task);
    return this;
  }

  @Override public String toString() {
    return method + " " + requestPath;
  }

  void setConsumer(Consumer<MockResponse> consumer) {
    this.consumer = consumer;
  }

  void setMockRouter(MockRouter mockRouter) {
    this.mockRouter = mockRouter;
  }
}
