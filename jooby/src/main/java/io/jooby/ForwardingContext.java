/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.jooby.exception.RegistryException;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

/**
 * Utility class that helps to wrap and delegate to another context.
 *
 * @author edgar
 * @since 2.0.2
 */
public class ForwardingContext implements Context {

  /** Delegate/forwarding body. */
  public static class ForwardingBody implements Body {
    protected final Body delegate;

    /**
     * Creates a new instance.
     *
     * @param body Underlying body.
     */
    public ForwardingBody(Body body) {
      this.delegate = body;
    }

    @Override
    public String value(Charset charset) {
      return delegate.value(charset);
    }

    @Override
    public byte[] bytes() {
      return delegate.bytes();
    }

    @Override
    public boolean isInMemory() {
      return delegate.isInMemory();
    }

    @Override
    public long getSize() {
      return delegate.getSize();
    }

    @Override
    public ReadableByteChannel channel() {
      return delegate.channel();
    }

    @Override
    public InputStream stream() {
      return delegate.stream();
    }

    @Override
    public <T> List<T> toList(Class<T> type) {
      return delegate.toList(type);
    }

    @Override
    public List<String> toList() {
      return delegate.toList();
    }

    @Override
    public Set<String> toSet() {
      return delegate.toSet();
    }

    @Override
    public <T> T to(Class<T> type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(Class<T> type) {
      return delegate.toNullable(type);
    }

    @Override
    public <T> T to(Type type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(Type type) {
      return delegate.toNullable(type);
    }

    @Override
    public Value get(int index) {
      return delegate.get(index);
    }

    @Override
    public Value get(String name) {
      return delegate.get(name);
    }

    @Override
    public Value getOrDefault(String name, String defaultValue) {
      return delegate.getOrDefault(name, defaultValue);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public Iterator<Value> iterator() {
      return delegate.iterator();
    }

    @Override
    public String resolve(String expression) {
      return delegate.resolve(expression);
    }

    @Override
    public String resolve(String expression, boolean ignoreMissing) {
      return delegate.resolve(expression, ignoreMissing);
    }

    @Override
    public String resolve(String expression, String startDelim, String endDelim) {
      return delegate.resolve(expression, startDelim, endDelim);
    }

    @Override
    public String resolve(
        String expression, boolean ignoreMissing, String startDelim, String endDelim) {
      return delegate.resolve(expression, ignoreMissing, startDelim, endDelim);
    }

    @Override
    public void forEach(Consumer<? super Value> action) {
      delegate.forEach(action);
    }

    @Override
    public Spliterator<Value> spliterator() {
      return delegate.spliterator();
    }

    @Override
    public long longValue() {
      return delegate.longValue();
    }

    @Override
    public long longValue(long defaultValue) {
      return delegate.longValue(defaultValue);
    }

    @Override
    public int intValue() {
      return delegate.intValue();
    }

    @Override
    public int intValue(int defaultValue) {
      return delegate.intValue(defaultValue);
    }

    @Override
    public byte byteValue() {
      return delegate.byteValue();
    }

    @Override
    public byte byteValue(byte defaultValue) {
      return delegate.byteValue(defaultValue);
    }

    @Override
    public float floatValue() {
      return delegate.floatValue();
    }

    @Override
    public float floatValue(float defaultValue) {
      return delegate.floatValue(defaultValue);
    }

    @Override
    public double doubleValue() {
      return delegate.doubleValue();
    }

    @Override
    public double doubleValue(double defaultValue) {
      return delegate.doubleValue(defaultValue);
    }

    @Override
    public boolean booleanValue() {
      return delegate.booleanValue();
    }

    @Override
    public boolean booleanValue(boolean defaultValue) {
      return delegate.booleanValue(defaultValue);
    }

    @Override
    public String value(String defaultValue) {
      return delegate.value(defaultValue);
    }

    @Override
    @Nullable public String valueOrNull() {
      return delegate.valueOrNull();
    }

    @Override
    public <T> T value(SneakyThrows.Function<String, T> fn) {
      return delegate.value(fn);
    }

    @Override
    public String value() {
      return delegate.value();
    }

    @Override
    public <T extends Enum<T>> T toEnum(SneakyThrows.Function<String, T> fn) {
      return delegate.toEnum(fn);
    }

    @Override
    public <T extends Enum<T>> T toEnum(
        SneakyThrows.Function<String, T> fn, Function<String, String> nameProvider) {
      return delegate.toEnum(fn, nameProvider);
    }

    @Override
    public Optional<String> toOptional() {
      return delegate.toOptional();
    }

    @Override
    public boolean isSingle() {
      return delegate.isSingle();
    }

    @Override
    public boolean isMissing() {
      return delegate.isMissing();
    }

    @Override
    public boolean isPresent() {
      return delegate.isPresent();
    }

    @Override
    public boolean isArray() {
      return delegate.isArray();
    }

    @Override
    public boolean isObject() {
      return delegate.isObject();
    }

    @Override
    @Nullable public String name() {
      return delegate.name();
    }

    @Override
    public <T> Optional<T> toOptional(Class<T> type) {
      return delegate.toOptional(type);
    }

    @Override
    public <T> Set<T> toSet(Class<T> type) {
      return delegate.toSet(type);
    }

    @Override
    public Map<String, List<String>> toMultimap() {
      return delegate.toMultimap();
    }

    @Override
    public Map<String, String> toMap() {
      return delegate.toMap();
    }
  }

  /** Forwarding/Delegate pattern over {@link Value}. */
  public static class ForwardingValue implements Value {
    protected final Value delegate;

    /**
     * Creates a new instance.
     *
     * @param delegate Underlying value.
     */
    public ForwardingValue(Value delegate) {
      this.delegate = delegate;
    }

    @Override
    public Value get(int index) {
      return delegate.get(index);
    }

    @Override
    public Value get(String name) {
      return delegate.get(name);
    }

    @Override
    public Value getOrDefault(String name, String defaultValue) {
      return delegate.getOrDefault(name, defaultValue);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public Iterator<Value> iterator() {
      return delegate.iterator();
    }

    @Override
    public String resolve(String expression) {
      return delegate.resolve(expression);
    }

    @Override
    public String resolve(String expression, boolean ignoreMissing) {
      return delegate.resolve(expression, ignoreMissing);
    }

    @Override
    public String resolve(String expression, String startDelim, String endDelim) {
      return delegate.resolve(expression, startDelim, endDelim);
    }

    @Override
    public String resolve(
        String expression, boolean ignoreMissing, String startDelim, String endDelim) {
      return delegate.resolve(expression, ignoreMissing, startDelim, endDelim);
    }

    @Override
    public void forEach(Consumer<? super Value> action) {
      delegate.forEach(action);
    }

    @Override
    public Spliterator<Value> spliterator() {
      return delegate.spliterator();
    }

    @Override
    public long longValue() {
      return delegate.longValue();
    }

    @Override
    public long longValue(long defaultValue) {
      return delegate.longValue(defaultValue);
    }

    @Override
    public int intValue() {
      return delegate.intValue();
    }

    @Override
    public int intValue(int defaultValue) {
      return delegate.intValue(defaultValue);
    }

    @Override
    public byte byteValue() {
      return delegate.byteValue();
    }

    @Override
    public byte byteValue(byte defaultValue) {
      return delegate.byteValue(defaultValue);
    }

    @Override
    public float floatValue() {
      return delegate.floatValue();
    }

    @Override
    public float floatValue(float defaultValue) {
      return delegate.floatValue(defaultValue);
    }

    @Override
    public double doubleValue() {
      return delegate.doubleValue();
    }

    @Override
    public double doubleValue(double defaultValue) {
      return delegate.doubleValue(defaultValue);
    }

    @Override
    public boolean booleanValue() {
      return delegate.booleanValue();
    }

    @Override
    public boolean booleanValue(boolean defaultValue) {
      return delegate.booleanValue(defaultValue);
    }

    @Override
    public String value(String defaultValue) {
      return delegate.value(defaultValue);
    }

    @Override
    @Nullable public String valueOrNull() {
      return delegate.valueOrNull();
    }

    @Override
    public <T> T value(SneakyThrows.Function<String, T> fn) {
      return delegate.value(fn);
    }

    @Override
    public String value() {
      return delegate.value();
    }

    @Override
    public List<String> toList() {
      return delegate.toList();
    }

    @Override
    public Set<String> toSet() {
      return delegate.toSet();
    }

    @Override
    public <T extends Enum<T>> T toEnum(SneakyThrows.Function<String, T> fn) {
      return delegate.toEnum(fn);
    }

    @Override
    public <T extends Enum<T>> T toEnum(
        SneakyThrows.Function<String, T> fn, Function<String, String> nameProvider) {
      return delegate.toEnum(fn, nameProvider);
    }

    @Override
    public Optional<String> toOptional() {
      return delegate.toOptional();
    }

    @Override
    public boolean isSingle() {
      return delegate.isSingle();
    }

    @Override
    public boolean isMissing() {
      return delegate.isMissing();
    }

    @Override
    public boolean isPresent() {
      return delegate.isPresent();
    }

    @Override
    public boolean isArray() {
      return delegate.isArray();
    }

    @Override
    public boolean isObject() {
      return delegate.isObject();
    }

    @Override
    @Nullable public String name() {
      return delegate.name();
    }

    @Override
    public <T> Optional<T> toOptional(Class<T> type) {
      return delegate.toOptional(type);
    }

    @Override
    public <T> List<T> toList(Class<T> type) {
      return delegate.toList(type);
    }

    @Override
    public <T> Set<T> toSet(Class<T> type) {
      return delegate.toSet(type);
    }

    @Override
    public <T> T to(Class<T> type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(Class<T> type) {
      return delegate.toNullable(type);
    }

    @Override
    public Map<String, List<String>> toMultimap() {
      return delegate.toMultimap();
    }

    @Override
    public Map<String, String> toMap() {
      return delegate.toMap();
    }
  }

  /** Forwarding/Delegate pattern over {@link QueryString}. */
  public static class ForwardingQueryString extends ForwardingValue implements QueryString {
    /**
     * Creates a new instance.
     *
     * @param queryString Underlying query string.
     */
    public ForwardingQueryString(QueryString queryString) {
      super(queryString);
    }

    @Override
    public <T> T toEmpty(Class<T> type) {
      return ((QueryString) delegate).toEmpty(type);
    }

    @Override
    public String queryString() {
      return ((QueryString) delegate).queryString();
    }
  }

  /** Forwarding/Delegate pattern over {@link Formdata}. */
  public static class ForwardingFormdata extends ForwardingValue implements Formdata {
    /**
     * Creates a new instance.
     *
     * @param delegate Underlying formdata.
     */
    public ForwardingFormdata(Formdata delegate) {
      super(delegate);
    }

    @Override
    public void put(String path, Value value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(String path, String value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(String path, Collection<String> values) {
      ((Formdata) delegate).put(path, values);
    }

    @Override
    public void put(String name, FileUpload file) {
      ((Formdata) delegate).put(name, file);
    }

    @Override
    public List<FileUpload> files() {
      return ((Formdata) delegate).files();
    }

    @Override
    public List<FileUpload> files(String name) {
      return ((Formdata) delegate).files(name);
    }

    @Override
    public FileUpload file(String name) {
      return ((Formdata) delegate).file(name);
    }
  }

  protected final Context ctx;

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ForwardingContext(Context context) {
    this.ctx = context;
  }

  @Nullable @Override
  public <T> T getUser() {
    return ctx.getUser();
  }

  @Override
  public Context setUser(@Nullable Object user) {
    ctx.setUser(user);
    return this;
  }

  /**
   * Get the underlying context.
   *
   * @return Get the underlying context.
   */
  public Context getDelegate() {
    return ctx;
  }

  @Override
  public Object forward(String path) {
    Object result = ctx.forward(path);
    if (result instanceof Context) {
      return this;
    }
    return result;
  }

  @Override
  public boolean matches(String pattern) {
    return ctx.matches(pattern);
  }

  @Override
  public boolean isSecure() {
    return ctx.isSecure();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return ctx.getAttributes();
  }

  @Nullable @Override
  public <T> T getAttribute(String key) {
    return ctx.getAttribute(key);
  }

  @Override
  public Context setAttribute(String key, Object value) {
    ctx.setAttribute(key, value);
    return this;
  }

  @Override
  public Router getRouter() {
    return ctx.getRouter();
  }

  @Override
  public OutputFactory getOutputFactory() {
    return ctx.getOutputFactory().getContextFactory();
  }

  @Override
  public FlashMap flash() {
    return ctx.flash();
  }

  @Nullable @Override
  public FlashMap flashOrNull() {
    return ctx.flashOrNull();
  }

  @Override
  public Value flash(String name) {
    return ctx.flash(name);
  }

  @Override
  public Value flash(String name, String defaultValue) {
    return ctx.flash(name, defaultValue);
  }

  @Override
  public Value session(String name) {
    return ctx.session(name);
  }

  @Override
  public Value session(String name, String defaultValue) {
    return ctx.session(name, defaultValue);
  }

  @Override
  public Session session() {
    return ctx.session();
  }

  @Nullable @Override
  public Session sessionOrNull() {
    return ctx.sessionOrNull();
  }

  @Override
  public Value cookie(String name) {
    return ctx.cookie(name);
  }

  @Override
  public Value cookie(String name, String defaultValue) {
    return ctx.cookie(name, defaultValue);
  }

  @Override
  public Map<String, String> cookieMap() {
    return ctx.cookieMap();
  }

  @Override
  public String getMethod() {
    return ctx.getMethod();
  }

  @Override
  public Context setMethod(String method) {
    ctx.setMethod(method);
    return this;
  }

  @Override
  public Route getRoute() {
    return ctx.getRoute();
  }

  @Override
  public Context setRoute(Route route) {
    return ctx.setRoute(route);
  }

  @Override
  public String getRequestPath() {
    return ctx.getRequestPath();
  }

  @Override
  public Context setRequestPath(String path) {
    ctx.setRequestPath(path);
    return this;
  }

  @Override
  public ParamLookup lookup() {
    return ctx.lookup();
  }

  @Override
  public Value lookup(String name, ParamSource... sources) {
    return ctx.lookup(name, sources);
  }

  @Override
  public Value path(String name) {
    return ctx.path(name);
  }

  @Override
  public <T> T path(Class<T> type) {
    return ctx.path(type);
  }

  @Override
  public Value path() {
    return ctx.path();
  }

  @Override
  public Map<String, String> pathMap() {
    return ctx.pathMap();
  }

  @Override
  public Context setPathMap(Map<String, String> pathMap) {
    ctx.setPathMap(pathMap);
    return this;
  }

  @Override
  public QueryString query() {
    return ctx.query();
  }

  @Override
  public Value query(String name) {
    return ctx.query(name);
  }

  @Override
  public Value query(String name, String defaultValue) {
    return ctx.query(name, defaultValue);
  }

  @Override
  public String queryString() {
    return ctx.queryString();
  }

  @Override
  public <T> T query(Class<T> type) {
    return ctx.query(type);
  }

  @Override
  public Map<String, String> queryMap() {
    return ctx.queryMap();
  }

  @Override
  public Value header() {
    return ctx.header();
  }

  @Override
  public Value header(String name) {
    return ctx.header(name);
  }

  @Override
  public Value header(String name, String defaultValue) {
    return ctx.header(name, defaultValue);
  }

  @Override
  public Map<String, String> headerMap() {
    return ctx.headerMap();
  }

  @Override
  public boolean accept(MediaType contentType) {
    return ctx.accept(contentType);
  }

  @Nullable @Override
  public MediaType accept(List<MediaType> produceTypes) {
    return ctx.accept(produceTypes);
  }

  @Nullable @Override
  public MediaType getRequestType() {
    return ctx.getRequestType();
  }

  @Override
  public MediaType getRequestType(MediaType defaults) {
    return ctx.getRequestType(defaults);
  }

  @Override
  public long getRequestLength() {
    return ctx.getRequestLength();
  }

  @Override
  public String getRemoteAddress() {
    return ctx.getRemoteAddress();
  }

  @Override
  public Context setRemoteAddress(String remoteAddress) {
    ctx.setRemoteAddress(remoteAddress);
    return this;
  }

  @Override
  public String getHost() {
    return ctx.getHost();
  }

  @Override
  public Context setHost(String host) {
    ctx.setHost(host);
    return this;
  }

  @Override
  public int getServerPort() {
    return ctx.getServerPort();
  }

  @Override
  public String getServerHost() {
    return ctx.getServerHost();
  }

  @Override
  public int getPort() {
    return ctx.getPort();
  }

  @Override
  public Context setPort(int port) {
    this.ctx.setPort(port);
    return this;
  }

  @Override
  public String getHostAndPort() {
    return ctx.getHostAndPort();
  }

  @Override
  public String getRequestURL() {
    return ctx.getRequestURL();
  }

  @Override
  public String getRequestURL(String path) {
    return ctx.getRequestURL(path);
  }

  @Override
  public String getProtocol() {
    return ctx.getProtocol();
  }

  @Override
  public List<Certificate> getClientCertificates() {
    return ctx.getClientCertificates();
  }

  @Override
  public String getScheme() {
    return ctx.getScheme();
  }

  @Override
  public Context setScheme(String scheme) {
    this.ctx.setScheme(scheme);
    return this;
  }

  @Override
  public Formdata form() {
    return ctx.form();
  }

  @Override
  public Value form(String name) {
    return ctx.form(name);
  }

  @Override
  public Value form(String name, String defaultValue) {
    return ctx.form(name, defaultValue);
  }

  @Override
  public <T> T form(Class<T> type) {
    return ctx.form(type);
  }

  @Override
  public Map<String, String> formMap() {
    return ctx.formMap();
  }

  @Override
  public List<FileUpload> files() {
    return ctx.files();
  }

  @Override
  public List<FileUpload> files(String name) {
    return ctx.files(name);
  }

  @Override
  public FileUpload file(String name) {
    return ctx.file(name);
  }

  @Override
  public Body body() {
    return ctx.body();
  }

  @Override
  public <T> T body(Class<T> type) {
    return ctx.body(type);
  }

  @Override
  public <T> T body(Type type) {
    return ctx.body(type);
  }

  @Override
  public ValueFactory getValueFactory() {
    return ctx.getValueFactory();
  }

  @Override
  public <T> T decode(Type type, MediaType contentType) {
    return ctx.decode(type, contentType);
  }

  @Override
  public MessageDecoder decoder(MediaType contentType) {
    return ctx.decoder(contentType);
  }

  @Override
  public boolean isInIoThread() {
    return ctx.isInIoThread();
  }

  @Override
  public Context dispatch(Runnable action) {
    ctx.dispatch(action);
    return this;
  }

  @Override
  public Context dispatch(Executor executor, Runnable action) {
    ctx.dispatch(executor, action);
    return this;
  }

  @Override
  public Context upgrade(WebSocket.Initializer handler) {
    ctx.upgrade(handler);
    return this;
  }

  @Override
  public Context upgrade(ServerSentEmitter.Handler handler) {
    ctx.upgrade(handler);
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Date value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Instant value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  public Context setResponseHeader(String name, Object value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  public Context setResponseHeader(String name, String value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  public Context removeResponseHeader(String name) {
    ctx.removeResponseHeader(name);
    return this;
  }

  @Override
  public Context removeResponseHeaders() {
    ctx.removeResponseHeaders();
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(String name) {
    return ctx.getResponseHeader(name);
  }

  @Override
  public long getResponseLength() {
    return ctx.getResponseLength();
  }

  @Override
  public Context setResponseLength(long length) {
    ctx.setResponseLength(length);
    return this;
  }

  @Override
  public Context setResponseCookie(Cookie cookie) {
    ctx.setResponseCookie(cookie);
    return this;
  }

  @Override
  public Context setResponseType(String contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override
  public Context setResponseType(MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override
  public Context setDefaultResponseType(MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override
  public MediaType getResponseType() {
    return ctx.getResponseType();
  }

  @Override
  public Context setResponseCode(StatusCode statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override
  public Context setResponseCode(int statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override
  public StatusCode getResponseCode() {
    return ctx.getResponseCode();
  }

  @Override
  public Context render(Object value) {
    ctx.render(value);
    return this;
  }

  @Override
  public OutputStream responseStream() {
    return ctx.responseStream();
  }

  @Override
  public OutputStream responseStream(MediaType contentType) {
    return ctx.responseStream(contentType);
  }

  @Override
  public Context responseStream(MediaType contentType, SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    return ctx.responseStream(contentType, consumer);
  }

  @Override
  public Context responseStream(SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    return ctx.responseStream(consumer);
  }

  @Override
  public Sender responseSender() {
    return ctx.responseSender();
  }

  @Override
  public PrintWriter responseWriter() {
    return ctx.responseWriter();
  }

  @Override
  public PrintWriter responseWriter(MediaType contentType) {
    return ctx.responseWriter(contentType);
  }

  @Override
  public Context responseWriter(SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return ctx.responseWriter(consumer);
  }

  @Override
  public Context responseWriter(MediaType contentType, SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return ctx.responseWriter(contentType, consumer);
  }

  @Override
  public Context sendRedirect(String location) {
    ctx.sendRedirect(location);
    return this;
  }

  @Override
  public Context sendRedirect(StatusCode redirect, String location) {
    ctx.sendRedirect(redirect, location);
    return this;
  }

  @Override
  public Context send(String data) {
    ctx.send(data);
    return this;
  }

  @Override
  public Context send(String data, Charset charset) {
    ctx.send(data, charset);
    return this;
  }

  @Override
  public Context send(byte[] data) {
    ctx.send(data);
    return this;
  }

  @Override
  public Context send(ByteBuffer data) {
    ctx.send(data);
    return this;
  }

  @Override
  public Context send(Output output) {
    ctx.send(output);
    return this;
  }

  @Override
  public Context send(byte[]... data) {
    ctx.send(data);
    return this;
  }

  @Override
  public Context send(ByteBuffer[] data) {
    ctx.send(data);
    return this;
  }

  @Override
  public Context send(ReadableByteChannel channel) {
    ctx.send(channel);
    return this;
  }

  @Override
  public Context send(InputStream input) {
    ctx.send(input);
    return this;
  }

  @Override
  public Context send(FileDownload file) {
    ctx.send(file);
    return this;
  }

  @Override
  public Context send(Path file) {
    ctx.send(file);
    return this;
  }

  @Override
  public Context send(FileChannel file) {
    ctx.send(file);
    return this;
  }

  @Override
  public Context send(StatusCode statusCode) {
    ctx.send(statusCode);
    return this;
  }

  @Override
  public Context sendError(Throwable cause) {
    ctx.sendError(cause);
    return this;
  }

  @Override
  public Context sendError(Throwable cause, StatusCode code) {
    ctx.sendError(cause, code);
    return this;
  }

  @Override
  public boolean isResponseStarted() {
    return ctx.isResponseStarted();
  }

  @Override
  public boolean getResetHeadersOnError() {
    return ctx.getResetHeadersOnError();
  }

  @Override
  public Context setResetHeadersOnError(boolean value) {
    ctx.setResetHeadersOnError(value);
    return this;
  }

  @Override
  public Context onComplete(Route.Complete task) {
    ctx.onComplete(task);
    return this;
  }

  @Override
  public <T> T require(Class<T> type) throws RegistryException {
    return ctx.require(type);
  }

  @Override
  public <T> T require(Class<T> type, String name) throws RegistryException {
    return ctx.require(type, name);
  }

  @Override
  public <T> T require(Reified<T> type) throws RegistryException {
    return ctx.require(type);
  }

  @Override
  public <T> T require(Reified<T> type, String name) throws RegistryException {
    return ctx.require(type, name);
  }

  @Override
  public <T> T require(ServiceKey<T> key) throws RegistryException {
    return ctx.require(key);
  }

  @Override
  public String toString() {
    return ctx.toString();
  }
}
