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

import org.jspecify.annotations.Nullable;

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
import io.jooby.WebSocket;
import io.jooby.exception.TypeMismatchException;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;
import io.jooby.value.Value;
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

  private OutputFactory outputFactory = OutputFactory.create(OutputOptions.small());

  /** Default constructor. */
  public MockContext() {}

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public OutputFactory getOutputFactory() {
    return outputFactory;
  }

  /**
   * Set HTTP method.
   *
   * @param method HTTP method.
   * @return This context.
   */
  @Override
  public MockContext setMethod(String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @Override
  public Session session() {
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
  public MockContext setSession(MockSession session) {
    this.session = session;
    return this;
  }

  @Nullable @Override
  public Session sessionOrNull() {
    return session;
  }

  @Override
  public Map<String, String> cookieMap() {
    return cookies;
  }

  @Override
  public Object forward(String path) {
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
  public MockContext setCookieMap(Map<String, String> cookies) {
    this.cookies = cookies;
    return this;
  }

  @Override
  public FlashMap flash() {
    return flashMap;
  }

  /**
   * Set flash map.
   *
   * @param flashMap Flash map.
   * @return This context.
   */
  public MockContext setFlashMap(FlashMap flashMap) {
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
  public MockContext setFlashAttribute(String name, String value) {
    flashMap.put(name, value);
    return this;
  }

  @Override
  public Route getRoute() {
    return route;
  }

  @Override
  public MockContext setRoute(Route route) {
    this.route = route;
    return this;
  }

  @Override
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
  public MockContext setRequestPath(String pathString) {
    int q = pathString.indexOf("?");
    if (q > 0) {
      this.requestPath = pathString.substring(0, q);
    } else {
      this.requestPath = pathString;
    }
    return this;
  }

  @Override
  public Map<String, String> pathMap() {
    return pathMap;
  }

  @Override
  public MockContext setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override
  public QueryString query() {
    return QueryString.create(valueFactory, queryString);
  }

  @Override
  public String queryString() {
    return queryString;
  }

  /**
   * Set query string value.
   *
   * @param queryString Query string (starting with <code>?</code>).
   * @return This context.
   */
  public MockContext setQueryString(String queryString) {
    this.queryString = queryString;
    return this;
  }

  @Override
  public Value header() {
    return Value.headers(valueFactory, headers);
  }

  /**
   * Set request headers.
   *
   * @param headers Request headers.
   * @return This context.
   */
  public MockContext setHeaders(Map<String, Collection<String>> headers) {
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
  public MockContext setRequestHeader(String name, String value) {
    Collection<String> values = this.headers.computeIfAbsent(name, k -> new ArrayList<>());
    values.add(value);
    return this;
  }

  @Override
  public Formdata form() {
    return formdata;
  }

  @Override
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
  public MockContext setFile(String name, FileUpload file) {
    this.files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
    return this;
  }

  @Override
  public List<FileUpload> files(String name) {
    return files.entrySet().stream()
        .filter(it -> it.getKey().equals(name))
        .flatMap(it -> it.getValue().stream())
        .collect(Collectors.toList());
  }

  @Override
  public FileUpload file(String name) {
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
  public MockContext setForm(Formdata formdata) {
    this.formdata = formdata;
    return this;
  }

  @Override
  public Body body() {
    if (body == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    return body;
  }

  @Override
  public <T> T body(Class<T> type) {
    return decode(type, MediaType.text);
  }

  @Override
  public <T> T body(Type type) {
    return decode(type, MediaType.text);
  }

  @Override
  public <T> T decode(Type type, MediaType contentType) {
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
  public MockContext setBody(Body body) {
    this.body = body;
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  public MockContext setBodyObject(Object body) {
    this.bodyObject = body;
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  public MockContext setBody(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return setBody(bytes);
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  public MockContext setBody(byte[] body) {
    setBody(Body.of(this, new ByteArrayInputStream(body), body.length));
    return this;
  }

  @Override
  public MessageDecoder decoder(MediaType contentType) {
    return decoders.getOrDefault(contentType, MessageDecoder.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override
  public boolean isInIoThread() {
    return false;
  }

  @Override
  public MockContext dispatch(Runnable action) {
    action.run();
    return this;
  }

  @Override
  public MockContext dispatch(Executor executor, Runnable action) {
    action.run();
    return this;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public MockContext removeResponseHeader(String name) {
    responseHeaders.remove(name);
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(String name) {
    Object value = responseHeaders.get(name);
    return value == null ? null : value.toString();
  }

  @Override
  public MockContext setResponseHeader(String name, String value) {
    responseHeaders.put(name, value);
    return this;
  }

  @Override
  public MockContext setResponseLength(long length) {
    response.setContentLength(length);
    return this;
  }

  @Override
  public long getResponseLength() {
    return response.getContentLength();
  }

  @Override
  public MockContext setResponseType(String contentType) {
    response.setContentType(MediaType.valueOf(contentType));
    return this;
  }

  @Override
  public MockContext setResponseType(MediaType contentType) {
    response.setContentType(contentType);
    return this;
  }

  @Override
  public MockContext setResponseCode(int statusCode) {
    response.setStatusCode(StatusCode.valueOf(statusCode));
    return this;
  }

  @Override
  public StatusCode getResponseCode() {
    return response.getStatusCode();
  }

  @Override
  public MockContext render(Object result) {
    responseStarted = true;
    this.response.setResult(result);
    return this;
  }

  /**
   * Mock response generated from route execution.
   *
   * @return Mock response.
   */
  public MockResponse getResponse() {
    response.setHeaders(responseHeaders);
    return response;
  }

  @Override
  public OutputStream responseStream() {
    responseStarted = true;
    ByteArrayOutputStream out = new ByteArrayOutputStream(ServerOptions._16KB);
    this.response.setResult(out);
    return out;
  }

  @Override
  public Sender responseSender() {
    responseStarted = true;
    return new Sender() {
      @Override
      public Sender write(byte[] data, Callback callback) {
        response.setResult(data);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @Override
      public Sender write(Output output, Callback callback) {
        response.setResult(output);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @Override
      public void close() {
        listeners.run(MockContext.this);
      }
    };
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public Context setHost(String host) {
    this.host = host;
    return this;
  }

  @Override
  public String getRemoteAddress() {
    return remoteAddress;
  }

  @Override
  public Context setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @Override
  public List<Certificate> getClientCertificates() {
    return new ArrayList<Certificate>();
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public Context setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  public PrintWriter responseWriter(MediaType type) {
    responseStarted = true;
    PrintWriter writer = new PrintWriter(new StringWriter());
    this.response.setResult(writer).setContentType(type);
    return writer;
  }

  @Override
  public MockContext send(String data, Charset charset) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.length());
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(byte[] data) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.length);
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(byte[]... data) {
    responseStarted = true;
    this.response
        .setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].length).sum());
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(ByteBuffer data) {
    responseStarted = true;
    this.response.setResult(data).setContentLength(data.remaining());
    listeners.run(this);
    return this;
  }

  @Override
  public Context send(Output output) {
    responseStarted = true;
    this.response.setResult(output).setContentLength(output.size());
    listeners.run(this);
    return this;
  }

  @Override
  public Context send(ByteBuffer[] data) {
    responseStarted = true;
    this.response
        .setResult(data)
        .setContentLength(IntStream.range(0, data.length).map(i -> data[i].remaining()).sum());
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(InputStream input) {
    responseStarted = true;
    this.response.setResult(input);
    listeners.run(this);
    return this;
  }

  @Override
  public Context send(FileDownload file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Override
  public Context send(Path file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(ReadableByteChannel channel) {
    responseStarted = true;
    this.response.setResult(channel);
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(FileChannel file) {
    responseStarted = true;
    this.response.setResult(file);
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext send(StatusCode statusCode) {
    responseStarted = true;
    this.response.setContentLength(0).setStatusCode(statusCode);
    return this;
  }

  @Override
  public MockContext sendError(Throwable cause) {
    return sendError(cause, router.errorCode(cause));
  }

  @Override
  public MockContext sendError(Throwable cause, StatusCode code) {
    responseStarted = true;
    this.response.setResult(cause).setStatusCode(router.errorCode(cause));
    listeners.run(this);
    return this;
  }

  @Override
  public MockContext setDefaultResponseType(MediaType contentType) {
    response.setContentType(contentType);
    return this;
  }

  @Override
  public MockContext setResponseCookie(Cookie cookie) {
    String setCookie = (String) response.getHeaders().get("Set-Cookie");
    if (setCookie == null) {
      setCookie = cookie.toCookieString();
    } else {
      setCookie += ";" + cookie.toCookieString();
    }
    response.setHeader("Set-Cookie", setCookie);
    return this;
  }

  @Override
  public MediaType getResponseType() {
    return response.getContentType();
  }

  @Override
  public MockContext setResponseCode(StatusCode statusCode) {
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

  @Override
  public Context removeResponseHeaders() {
    responseHeaders.clear();
    return this;
  }

  @Override
  public Router getRouter() {
    return router;
  }

  /**
   * Set a mock router.
   *
   * @param router Mock router.
   * @return This context.
   */
  public MockContext setRouter(Router router) {
    this.router = router;
    return this;
  }

  @Override
  public MockContext upgrade(WebSocket.Initializer handler) {
    return this;
  }

  @Override
  public Context upgrade(ServerSentEmitter.Handler handler) {
    return this;
  }

  @Override
  public Context onComplete(Route.Complete task) {
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

  /**
   * Set/Override value factory.
   *
   * @param valueFactory Value Factory.
   */
  public void setValueFactory(ValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }
}
