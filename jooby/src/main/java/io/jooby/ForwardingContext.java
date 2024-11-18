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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.DataBuffer;
import io.jooby.buffer.DataBufferFactory;
import io.jooby.exception.RegistryException;

/**
 * Utility class that helps to wrap and delegate to another context.
 *
 * @author edgar
 * @since 2.0.2
 */
public class ForwardingContext implements Context {

  public static class ForwardingBody implements Body {
    protected final Body delegate;

    public ForwardingBody(Body body) {
      this.delegate = body;
    }

    @Override
    @NonNull public String value(@NonNull Charset charset) {
      return delegate.value(charset);
    }

    @Override
    @NonNull public byte[] bytes() {
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
    @NonNull public ReadableByteChannel channel() {
      return delegate.channel();
    }

    @Override
    @NonNull public InputStream stream() {
      return delegate.stream();
    }

    @Override
    @NonNull public <T> List<T> toList(@NonNull Class<T> type) {
      return delegate.toList(type);
    }

    @Override
    @NonNull public List<String> toList() {
      return delegate.toList();
    }

    @Override
    @NonNull public Set<String> toSet() {
      return delegate.toSet();
    }

    @Override
    @NonNull public <T> T to(@NonNull Class<T> type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(@NonNull Class<T> type) {
      return delegate.toNullable(type);
    }

    @Override
    @NonNull public <T> T to(@NonNull Type type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(@NonNull Type type) {
      return delegate.toNullable(type);
    }

    @Override
    @NonNull public ValueNode get(int index) {
      return delegate.get(index);
    }

    @Override
    @NonNull public ValueNode get(@NonNull String name) {
      return delegate.get(name);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    @NonNull public Iterator<ValueNode> iterator() {
      return delegate.iterator();
    }

    @Override
    @NonNull public String resolve(@NonNull String expression) {
      return delegate.resolve(expression);
    }

    @Override
    @NonNull public String resolve(@NonNull String expression, boolean ignoreMissing) {
      return delegate.resolve(expression, ignoreMissing);
    }

    @Override
    @NonNull public String resolve(
        @NonNull String expression, @NonNull String startDelim, @NonNull String endDelim) {
      return delegate.resolve(expression, startDelim, endDelim);
    }

    @Override
    @NonNull public String resolve(
        @NonNull String expression,
        boolean ignoreMissing,
        @NonNull String startDelim,
        @NonNull String endDelim) {
      return delegate.resolve(expression, ignoreMissing, startDelim, endDelim);
    }

    @Override
    public void forEach(Consumer<? super ValueNode> action) {
      delegate.forEach(action);
    }

    @Override
    public Spliterator<ValueNode> spliterator() {
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
    @NonNull public String value(@NonNull String defaultValue) {
      return delegate.value(defaultValue);
    }

    @Override
    @Nullable public String valueOrNull() {
      return delegate.valueOrNull();
    }

    @Override
    @NonNull public <T> T value(@NonNull SneakyThrows.Function<String, T> fn) {
      return delegate.value(fn);
    }

    @Override
    @NonNull public String value() {
      return delegate.value();
    }

    @Override
    @NonNull public <T extends Enum<T>> T toEnum(@NonNull SneakyThrows.Function<String, T> fn) {
      return delegate.toEnum(fn);
    }

    @Override
    @NonNull public <T extends Enum<T>> T toEnum(
        @NonNull SneakyThrows.Function<String, T> fn,
        @NonNull Function<String, String> nameProvider) {
      return delegate.toEnum(fn, nameProvider);
    }

    @Override
    @NonNull public Optional<String> toOptional() {
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
    @NonNull public <T> Optional<T> toOptional(@NonNull Class<T> type) {
      return delegate.toOptional(type);
    }

    @Override
    @NonNull public <T> Set<T> toSet(@NonNull Class<T> type) {
      return delegate.toSet(type);
    }

    @Override
    @NonNull public Map<String, List<String>> toMultimap() {
      return delegate.toMultimap();
    }

    @Override
    @NonNull public Map<String, String> toMap() {
      return delegate.toMap();
    }
  }

  public static class ForwardingValueNode implements ValueNode {
    protected final ValueNode delegate;

    public ForwardingValueNode(ValueNode delegate) {
      this.delegate = delegate;
    }

    @Override
    @NonNull public ValueNode get(@NonNull int index) {
      return delegate.get(index);
    }

    @Override
    @NonNull public ValueNode get(@NonNull String name) {
      return delegate.get(name);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    @NonNull public Iterator<ValueNode> iterator() {
      return delegate.iterator();
    }

    @Override
    @NonNull public String resolve(@NonNull String expression) {
      return delegate.resolve(expression);
    }

    @Override
    @NonNull public String resolve(@NonNull String expression, boolean ignoreMissing) {
      return delegate.resolve(expression, ignoreMissing);
    }

    @Override
    @NonNull public String resolve(
        @NonNull String expression, @NonNull String startDelim, @NonNull String endDelim) {
      return delegate.resolve(expression, startDelim, endDelim);
    }

    @Override
    @NonNull public String resolve(
        @NonNull String expression,
        boolean ignoreMissing,
        @NonNull String startDelim,
        @NonNull String endDelim) {
      return delegate.resolve(expression, ignoreMissing, startDelim, endDelim);
    }

    @Override
    public void forEach(Consumer<? super ValueNode> action) {
      delegate.forEach(action);
    }

    @Override
    public Spliterator<ValueNode> spliterator() {
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
    @NonNull public String value(@NonNull String defaultValue) {
      return delegate.value(defaultValue);
    }

    @Override
    @Nullable public String valueOrNull() {
      return delegate.valueOrNull();
    }

    @Override
    @NonNull public <T> T value(@NonNull SneakyThrows.Function<String, T> fn) {
      return delegate.value(fn);
    }

    @Override
    @NonNull public String value() {
      return delegate.value();
    }

    @Override
    @NonNull public List<String> toList() {
      return delegate.toList();
    }

    @Override
    @NonNull public Set<String> toSet() {
      return delegate.toSet();
    }

    @Override
    @NonNull public <T extends Enum<T>> T toEnum(@NonNull SneakyThrows.Function<String, T> fn) {
      return delegate.toEnum(fn);
    }

    @Override
    @NonNull public <T extends Enum<T>> T toEnum(
        @NonNull SneakyThrows.Function<String, T> fn,
        @NonNull Function<String, String> nameProvider) {
      return delegate.toEnum(fn, nameProvider);
    }

    @Override
    @NonNull public Optional<String> toOptional() {
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
    @NonNull public <T> Optional<T> toOptional(@NonNull Class<T> type) {
      return delegate.toOptional(type);
    }

    @Override
    @NonNull public <T> List<T> toList(@NonNull Class<T> type) {
      return delegate.toList(type);
    }

    @Override
    @NonNull public <T> Set<T> toSet(@NonNull Class<T> type) {
      return delegate.toSet(type);
    }

    @Override
    @NonNull public <T> T to(@NonNull Class<T> type) {
      return delegate.to(type);
    }

    @Override
    @Nullable public <T> T toNullable(@NonNull Class<T> type) {
      return delegate.toNullable(type);
    }

    @Override
    @NonNull public Map<String, List<String>> toMultimap() {
      return delegate.toMultimap();
    }

    @Override
    @NonNull public Map<String, String> toMap() {
      return delegate.toMap();
    }
  }

  public static class ForwardingQueryString extends ForwardingValueNode implements QueryString {
    public ForwardingQueryString(QueryString queryString) {
      super(queryString);
    }

    @NonNull @Override
    public String queryString() {
      return ((QueryString) delegate).queryString();
    }
  }

  public static class ForwardingFormdata extends ForwardingValueNode implements Formdata {
    public ForwardingFormdata(Formdata delegate) {
      super(delegate);
    }

    @Override
    public void put(@NonNull String path, @NonNull ValueNode value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(@NonNull String path, @NonNull String value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(@NonNull String path, @NonNull Collection<String> values) {
      ((Formdata) delegate).put(path, values);
    }

    @Override
    public void put(@NonNull String name, @NonNull FileUpload file) {
      ((Formdata) delegate).put(name, file);
    }

    @NonNull @Override
    public List<FileUpload> files() {
      return ((Formdata) delegate).files();
    }

    @NonNull @Override
    public List<FileUpload> files(@NonNull String name) {
      return ((Formdata) delegate).files(name);
    }

    @NonNull @Override
    public FileUpload file(@NonNull String name) {
      return ((Formdata) delegate).file(name);
    }
  }

  protected final Context ctx;

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ForwardingContext(@NonNull Context context) {
    this.ctx = context;
  }

  @Nullable @Override
  public <T> T getUser() {
    return ctx.getUser();
  }

  @NonNull @Override
  public Context setUser(@Nullable Object user) {
    ctx.setUser(user);
    return this;
  }

  public Context getDelegate() {
    return ctx;
  }

  @NonNull @Override
  public Object forward(@NonNull String path) {
    Object result = ctx.forward(path);
    if (result instanceof Context) {
      return this;
    }
    return result;
  }

  @Override
  public boolean matches(@NonNull String pattern) {
    return ctx.matches(pattern);
  }

  @Override
  public boolean isSecure() {
    return ctx.isSecure();
  }

  @Override
  @NonNull public Map<String, Object> getAttributes() {
    return ctx.getAttributes();
  }

  @Nullable @Override
  public <T> T getAttribute(@NonNull String key) {
    return ctx.getAttribute(key);
  }

  @NonNull @Override
  public Context setAttribute(@NonNull String key, Object value) {
    ctx.setAttribute(key, value);
    return this;
  }

  @Override
  @NonNull public Router getRouter() {
    return ctx.getRouter();
  }

  @NonNull @Override
  public DataBufferFactory getBufferFactory() {
    return ctx.getBufferFactory();
  }

  @NonNull @Override
  public FlashMap flash() {
    return ctx.flash();
  }

  @NonNull @Override
  public Value flash(@NonNull String name) {
    return ctx.flash(name);
  }

  @NonNull @Override
  public Value session(@NonNull String name) {
    return ctx.session(name);
  }

  @NonNull @Override
  public Session session() {
    return ctx.session();
  }

  @Nullable @Override
  public Session sessionOrNull() {
    return ctx.sessionOrNull();
  }

  @NonNull @Override
  public Value cookie(@NonNull String name) {
    return ctx.cookie(name);
  }

  @Override
  @NonNull public Map<String, String> cookieMap() {
    return ctx.cookieMap();
  }

  @Override
  @NonNull public String getMethod() {
    return ctx.getMethod();
  }

  @NonNull @Override
  public Context setMethod(@NonNull String method) {
    ctx.setMethod(method);
    return this;
  }

  @Override
  @NonNull public Route getRoute() {
    return ctx.getRoute();
  }

  @Override
  @NonNull public Context setRoute(@NonNull Route route) {
    return ctx.setRoute(route);
  }

  @NonNull @Override
  public String getRequestPath() {
    return ctx.getRequestPath();
  }

  @NonNull @Override
  public Context setRequestPath(@NonNull String path) {
    ctx.setRequestPath(path);
    return this;
  }

  @NonNull @Override
  public Value path(@NonNull String name) {
    return ctx.path(name);
  }

  @NonNull @Override
  public <T> T path(@NonNull Class<T> type) {
    return ctx.path(type);
  }

  @NonNull @Override
  public ValueNode path() {
    return ctx.path();
  }

  @Override
  @NonNull public Map<String, String> pathMap() {
    return ctx.pathMap();
  }

  @Override
  @NonNull public Context setPathMap(@NonNull Map<String, String> pathMap) {
    ctx.setPathMap(pathMap);
    return this;
  }

  @Override
  @NonNull public QueryString query() {
    return ctx.query();
  }

  @NonNull @Override
  public ValueNode query(@NonNull String name) {
    return ctx.query(name);
  }

  @NonNull @Override
  public String queryString() {
    return ctx.queryString();
  }

  @NonNull @Override
  public <T> T query(@NonNull Class<T> type) {
    return ctx.query(type);
  }

  @NonNull @Override
  public Map<String, String> queryMap() {
    return ctx.queryMap();
  }

  @Override
  @NonNull public ValueNode header() {
    return ctx.header();
  }

  @NonNull @Override
  public Value header(@NonNull String name) {
    return ctx.header(name);
  }

  @NonNull @Override
  public Map<String, String> headerMap() {
    return ctx.headerMap();
  }

  @Override
  public boolean accept(@NonNull MediaType contentType) {
    return ctx.accept(contentType);
  }

  @Nullable @Override
  public MediaType accept(@NonNull List<MediaType> produceTypes) {
    return ctx.accept(produceTypes);
  }

  @Nullable @Override
  public MediaType getRequestType() {
    return ctx.getRequestType();
  }

  @NonNull @Override
  public MediaType getRequestType(MediaType defaults) {
    return ctx.getRequestType(defaults);
  }

  @Override
  public long getRequestLength() {
    return ctx.getRequestLength();
  }

  @Override
  @NonNull public String getRemoteAddress() {
    return ctx.getRemoteAddress();
  }

  @NonNull @Override
  public Context setRemoteAddress(@NonNull String remoteAddress) {
    ctx.setRemoteAddress(remoteAddress);
    return this;
  }

  @NonNull @Override
  public String getHost() {
    return ctx.getHost();
  }

  @NonNull @Override
  public Context setHost(@NonNull String host) {
    ctx.setHost(host);
    return this;
  }

  @Override
  public int getServerPort() {
    return ctx.getServerPort();
  }

  @NonNull @Override
  public String getServerHost() {
    return ctx.getServerHost();
  }

  @Override
  public int getPort() {
    return ctx.getPort();
  }

  @NonNull @Override
  public Context setPort(int port) {
    this.ctx.setPort(port);
    return this;
  }

  @NonNull @Override
  public String getHostAndPort() {
    return ctx.getHostAndPort();
  }

  @NonNull @Override
  public String getRequestURL() {
    return ctx.getRequestURL();
  }

  @NonNull @Override
  public String getRequestURL(@NonNull String path) {
    return ctx.getRequestURL(path);
  }

  @Override
  @NonNull public String getProtocol() {
    return ctx.getProtocol();
  }

  @Override
  @NonNull public List<Certificate> getClientCertificates() {
    return ctx.getClientCertificates();
  }

  @Override
  @NonNull public String getScheme() {
    return ctx.getScheme();
  }

  @NonNull @Override
  public Context setScheme(@NonNull String scheme) {
    this.ctx.setScheme(scheme);
    return this;
  }

  @Override
  @NonNull public Formdata form() {
    return ctx.form();
  }

  @NonNull @Override
  public ValueNode form(@NonNull String name) {
    return ctx.form(name);
  }

  @NonNull @Override
  public <T> T form(@NonNull Class<T> type) {
    return ctx.form(type);
  }

  @NonNull @Override
  public Map<String, String> formMap() {
    return ctx.formMap();
  }

  @NonNull @Override
  public List<FileUpload> files() {
    return ctx.files();
  }

  @NonNull @Override
  public List<FileUpload> files(@NonNull String name) {
    return ctx.files(name);
  }

  @NonNull @Override
  public FileUpload file(@NonNull String name) {
    return ctx.file(name);
  }

  @Override
  @NonNull public Body body() {
    return ctx.body();
  }

  @NonNull @Override
  public <T> T body(@NonNull Class<T> type) {
    return ctx.body(type);
  }

  @NonNull @Override
  public <T> T body(@NonNull Type type) {
    return ctx.body(type);
  }

  @NonNull @Override
  public <T> T convert(@NonNull ValueNode value, @NonNull Class<T> type) {
    return ctx.convert(value, type);
  }

  @Nullable @Override
  public <T> T convertOrNull(@NonNull ValueNode value, @NonNull Class<T> type) {
    return ctx.convertOrNull(value, type);
  }

  @NonNull @Override
  public <T> T decode(@NonNull Type type, @NonNull MediaType contentType) {
    return ctx.decode(type, contentType);
  }

  @NonNull @Override
  public MessageDecoder decoder(@NonNull MediaType contentType) {
    return ctx.decoder(contentType);
  }

  @Override
  public boolean isInIoThread() {
    return ctx.isInIoThread();
  }

  @Override
  @NonNull public Context dispatch(@NonNull Runnable action) {
    ctx.dispatch(action);
    return this;
  }

  @Override
  @NonNull public Context dispatch(@NonNull Executor executor, @NonNull Runnable action) {
    ctx.dispatch(executor, action);
    return this;
  }

  @Override
  @NonNull public Context detach(@NonNull Route.Handler next) throws Exception {
    ctx.detach(next);
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull WebSocket.Initializer handler) {
    ctx.upgrade(handler);
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
    ctx.upgrade(handler);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Date value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Instant value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Object value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  @NonNull public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override
  @NonNull public Context removeResponseHeader(@NonNull String name) {
    ctx.removeResponseHeader(name);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    ctx.removeResponseHeaders();
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    return ctx.getResponseHeader(name);
  }

  @Override
  public long getResponseLength() {
    return ctx.getResponseLength();
  }

  @Override
  @NonNull public Context setResponseLength(long length) {
    ctx.setResponseLength(length);
    return this;
  }

  @Override
  @NonNull public Context setResponseCookie(@NonNull Cookie cookie) {
    ctx.setResponseCookie(cookie);
    return this;
  }

  @Override
  @NonNull public Context setResponseType(@NonNull String contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override
  @NonNull public Context setResponseType(@NonNull MediaType contentType, @Nullable Charset charset) {
    ctx.setResponseType(contentType, charset);
    return this;
  }

  @Override
  @NonNull public Context setDefaultResponseType(@NonNull MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override
  @NonNull public MediaType getResponseType() {
    return ctx.getResponseType();
  }

  @NonNull @Override
  public Context setResponseCode(@NonNull StatusCode statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override
  @NonNull public Context setResponseCode(int statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override
  @NonNull public StatusCode getResponseCode() {
    return ctx.getResponseCode();
  }

  @NonNull @Override
  public Context render(@NonNull Object value) {
    ctx.render(value);
    return this;
  }

  @Override
  @NonNull public OutputStream responseStream() {
    return ctx.responseStream();
  }

  @NonNull @Override
  public OutputStream responseStream(@NonNull MediaType contentType) {
    return ctx.responseStream(contentType);
  }

  @NonNull @Override
  public Context responseStream(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    return ctx.responseStream(contentType, consumer);
  }

  @NonNull @Override
  public Context responseStream(@NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    return ctx.responseStream(consumer);
  }

  @Override
  @NonNull public Sender responseSender() {
    return ctx.responseSender();
  }

  @NonNull @Override
  public PrintWriter responseWriter() {
    return ctx.responseWriter();
  }

  @NonNull @Override
  public PrintWriter responseWriter(@NonNull MediaType contentType) {
    return ctx.responseWriter(contentType);
  }

  @Override
  @NonNull public PrintWriter responseWriter(@NonNull MediaType contentType, @Nullable Charset charset) {
    return ctx.responseWriter(contentType, charset);
  }

  @NonNull @Override
  public Context responseWriter(@NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return ctx.responseWriter(consumer);
  }

  @NonNull @Override
  public Context responseWriter(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return ctx.responseWriter(contentType, consumer);
  }

  @NonNull @Override
  public Context responseWriter(
      @NonNull MediaType contentType,
      @Nullable Charset charset,
      @NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return ctx.responseWriter(contentType, charset, consumer);
  }

  @NonNull @Override
  public Context sendRedirect(@NonNull String location) {
    ctx.sendRedirect(location);
    return this;
  }

  @NonNull @Override
  public Context sendRedirect(@NonNull StatusCode redirect, @NonNull String location) {
    ctx.sendRedirect(redirect, location);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull String data) {
    ctx.send(data);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull String data, @NonNull Charset charset) {
    ctx.send(data, charset);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull byte[] data) {
    ctx.send(data);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull ByteBuffer data) {
    ctx.send(data);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull DataBuffer data) {
    ctx.send(data);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull byte[]... data) {
    ctx.send(data);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    ctx.send(data);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull ReadableByteChannel channel) {
    ctx.send(channel);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull InputStream input) {
    ctx.send(input);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull FileDownload file) {
    ctx.send(file);
    return this;
  }

  @NonNull @Override
  public Context send(@NonNull Path file) {
    ctx.send(file);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull FileChannel file) {
    ctx.send(file);
    return this;
  }

  @Override
  @NonNull public Context send(@NonNull StatusCode statusCode) {
    ctx.send(statusCode);
    return this;
  }

  @NonNull @Override
  public Context sendError(@NonNull Throwable cause) {
    ctx.sendError(cause);
    return this;
  }

  @NonNull @Override
  public Context sendError(@NonNull Throwable cause, @NonNull StatusCode code) {
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

  @NonNull @Override
  public Context onComplete(@NonNull Route.Complete task) {
    ctx.onComplete(task);
    return this;
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) throws RegistryException {
    return ctx.require(type);
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) throws RegistryException {
    return ctx.require(type, name);
  }

  @NonNull @Override
  public <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return ctx.require(key);
  }

  @Override
  public String toString() {
    return ctx.toString();
  }
}
