/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Utility to class that helps to wrap and delegate to another context.
 *
 * @since 2.0.2
 * @author edgar
 */
public class ForwardingContext implements Context {
  protected final Context ctx;

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ForwardingContext(@Nonnull Context context) {
    this.ctx = context;
  }

  @Override public boolean isSecure() {
    return ctx.isSecure();
  }

  @Override @Nonnull public Map<String, Object> getAttributes() {
    return ctx.getAttributes();
  }

  @Nonnull @Override public <T> T attribute(@Nonnull String key) {
    return ctx.attribute(key);
  }

  @Nonnull @Override public Context attribute(@Nonnull String key, Object value) {
    ctx.attribute(key, value);
    return this;
  }

  @Override @Nonnull public Router getRouter() {
    return ctx.getRouter();
  }

  @Nonnull @Override public FlashMap flash() {
    return ctx.flash();
  }

  @Nonnull @Override public Value flash(@Nonnull String name) {
    return ctx.flash(name);
  }

  @Nonnull @Override public Value session(@Nonnull String name) {
    return ctx.session(name);
  }

  @Nonnull @Override public Session session() {
    return ctx.session();
  }

  @Nullable @Override public Session sessionOrNull() {
    return ctx.sessionOrNull();
  }

  @Nonnull @Override public Value cookie(@Nonnull String name) {
    return ctx.cookie(name);
  }

  @Override @Nonnull public Map<String, String> cookieMap() {
    return ctx.cookieMap();
  }

  @Override @Nonnull public String getMethod() {
    return ctx.getMethod();
  }

  @Override @Nonnull public Route getRoute() {
    return ctx.getRoute();
  }

  @Override @Nonnull public Context setRoute(@Nonnull Route route) {
    return ctx.setRoute(route);
  }

  @Override @Nonnull public String pathString() {
    return ctx.pathString();
  }

  @Nonnull @Override public Value path(@Nonnull String name) {
    return ctx.path(name);
  }

  @Nonnull @Override public <T> T path(@Nonnull Class<T> type) {
    return ctx.path(type);
  }

  @Nonnull @Override public ValueNode path() {
    return ctx.path();
  }

  @Override @Nonnull public Map<String, String> pathMap() {
    return ctx.pathMap();
  }

  @Override @Nonnull public Context setPathMap(@Nonnull Map<String, String> pathMap) {
    ctx.setPathMap(pathMap);
    return this;
  }

  @Override @Nonnull public QueryString query() {
    return ctx.query();
  }

  @Nonnull @Override public ValueNode query(@Nonnull String name) {
    return ctx.query(name);
  }

  @Nonnull @Override public String queryString() {
    return ctx.queryString();
  }

  @Nonnull @Override public <T> T query(@Nonnull Class<T> type) {
    return ctx.query(type);
  }

  @Nonnull @Override public Map<String, String> queryMap() {
    return ctx.queryMap();
  }

  @Nonnull @Override public Map<String, List<String>> queryMultimap() {
    return ctx.queryMultimap();
  }

  @Override @Nonnull public ValueNode header() {
    return ctx.header();
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return ctx.header(name);
  }

  @Nonnull @Override public Map<String, String> headerMap() {
    return ctx.headerMap();
  }

  @Nonnull @Override public Map<String, List<String>> headerMultimap() {
    return ctx.headerMultimap();
  }

  @Override public boolean accept(@Nonnull MediaType contentType) {
    return ctx.accept(contentType);
  }

  @Nullable @Override public MediaType accept(@Nonnull List<MediaType> produceTypes) {
    return ctx.accept(produceTypes);
  }

  @Nullable @Override public MediaType getRequestType() {
    return ctx.getRequestType();
  }

  @Nonnull @Override public MediaType getRequestType(MediaType defaults) {
    return ctx.getRequestType(defaults);
  }

  @Override public long getRequestLength() {
    return ctx.getRequestLength();
  }

  @Override @Nonnull public String getRemoteAddress() {
    return ctx.getRemoteAddress();
  }

  @Nonnull @Override public String getHost() {
    return ctx.getHost();
  }

  @Override @Nonnull public String getProtocol() {
    return ctx.getProtocol();
  }

  @Override @Nonnull public String getScheme() {
    return ctx.getScheme();
  }

  @Override @Nonnull public Formdata form() {
    return ctx.form();
  }

  @Nonnull @Override public Map<String, List<String>> formMultimap() {
    return ctx.formMultimap();
  }

  @Nonnull @Override public Map<String, String> formMap() {
    return ctx.formMap();
  }

  @Nonnull @Override public ValueNode form(@Nonnull String name) {
    return ctx.form(name);
  }

  @Nonnull @Override public <T> T form(@Nonnull Class<T> type) {
    return ctx.form(type);
  }

  @Override @Nonnull public Multipart multipart() {
    return ctx.multipart();
  }

  @Nonnull @Override public ValueNode multipart(@Nonnull String name) {
    return ctx.multipart(name);
  }

  @Nonnull @Override public <T> T multipart(@Nonnull Class<T> type) {
    return ctx.multipart(type);
  }

  @Nonnull @Override public Map<String, List<String>> multipartMultimap() {
    return ctx.multipartMultimap();
  }

  @Nonnull @Override public Map<String, String> multipartMap() {
    return ctx.multipartMap();
  }

  @Nonnull @Override public List<FileUpload> files() {
    return ctx.files();
  }

  @Nonnull @Override public List<FileUpload> files(@Nonnull String name) {
    return ctx.files(name);
  }

  @Nonnull @Override public FileUpload file(@Nonnull String name) {
    return ctx.file(name);
  }

  @Override @Nonnull public Body body() {
    return ctx.body();
  }

  @Nonnull @Override public <T> T body(@Nonnull Class<T> type) {
    return ctx.body(type);
  }

  @Nonnull @Override public <T> T body(@Nonnull Type type) {
    return ctx.body(type);
  }

  @Nullable @Override public <T> T convert(ValueNode value, Class<T> type) {
    return ctx.convert(value, type);
  }

  @Nonnull @Override public <T> T decode(@Nonnull Type type, @Nonnull MediaType contentType) {
    return ctx.decode(type, contentType);
  }

  @Nonnull @Override public MessageDecoder decoder(@Nonnull MediaType contentType) {
    return ctx.decoder(contentType);
  }

  @Override public boolean isInIoThread() {
    return ctx.isInIoThread();
  }

  @Override @Nonnull public Context dispatch(@Nonnull Runnable action) {
    ctx.dispatch(action);
    return this;
  }

  @Override @Nonnull public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    ctx.dispatch(executor, action);
    return this;
  }

  @Override @Nonnull public Context detach(@Nonnull Route.Handler next) throws Exception {
    ctx.detach(next);
    return this;
  }

  @Nonnull @Override public Context upgrade(@Nonnull WebSocket.Initializer handler) {
    ctx.upgrade(handler);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Nonnull @Override
  public Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override @Nonnull public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    ctx.setResponseHeader(name, value);
    return this;
  }

  @Override @Nonnull public Context removeResponseHeader(@Nonnull String name) {
    ctx.removeResponseHeader(name);
    return this;
  }

  @Nonnull @Override public Context removeResponseHeaders() {
    ctx.removeResponseHeaders();
    return this;
  }

  @Override public long getResponseLength() {
    return ctx.getResponseLength();
  }

  @Override @Nonnull public Context setResponseLength(long length) {
    ctx.setResponseLength(length);
    return this;
  }

  @Override @Nonnull public Context setResponseCookie(@Nonnull Cookie cookie) {
    ctx.setResponseCookie(cookie);
    return this;
  }

  @Override @Nonnull public Context setResponseType(@Nonnull String contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override @Nonnull public Context setResponseType(@Nonnull MediaType contentType,
      @Nullable Charset charset) {
    ctx.setResponseType(contentType, charset);
    return this;
  }

  @Override @Nonnull public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    ctx.setResponseType(contentType);
    return this;
  }

  @Override @Nonnull public MediaType getResponseType() {
    return ctx.getResponseType();
  }

  @Nonnull @Override public Context setResponseCode(@Nonnull StatusCode statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override @Nonnull public Context setResponseCode(int statusCode) {
    ctx.setResponseCode(statusCode);
    return this;
  }

  @Override @Nonnull public StatusCode getResponseCode() {
    return ctx.getResponseCode();
  }

  @Nonnull @Override public Context render(@Nonnull Object value) {
    ctx.render(value);
    return this;
  }

  @Override @Nonnull public OutputStream responseStream() {
    return ctx.responseStream();
  }

  @Nonnull @Override public OutputStream responseStream(@Nonnull MediaType contentType) {
    return ctx.responseStream(contentType);
  }

  @Nonnull @Override public Context responseStream(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    return ctx.responseStream(contentType, consumer);
  }

  @Nonnull @Override
  public Context responseStream(@Nonnull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    return ctx.responseStream(consumer);
  }

  @Override @Nonnull public Sender responseSender() {
    return ctx.responseSender();
  }

  @Nonnull @Override public PrintWriter responseWriter() {
    return ctx.responseWriter();
  }

  @Nonnull @Override public PrintWriter responseWriter(@Nonnull MediaType contentType) {
    return ctx.responseWriter(contentType);
  }

  @Override @Nonnull public PrintWriter responseWriter(@Nonnull MediaType contentType,
      @Nullable Charset charset) {
    return ctx.responseWriter(contentType, charset);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return ctx.responseWriter(consumer);
  }

  @Nonnull @Override public Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return ctx.responseWriter(contentType, consumer);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return ctx.responseWriter(contentType, charset, consumer);
  }

  @Nonnull @Override public Context sendRedirect(@Nonnull String location) {
    ctx.sendRedirect(location);
    return this;
  }

  @Nonnull @Override
  public Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location) {
    ctx.sendRedirect(redirect, location);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    ctx.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull String data, @Nonnull Charset charset) {
    ctx.send(data, charset);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull byte[] data) {
    ctx.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull ByteBuffer data) {
    ctx.send(data);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull byte[]... data) {
    ctx.send(data);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer[] data) {
    ctx.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull ReadableByteChannel channel) {
    ctx.send(channel);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull InputStream input) {
    ctx.send(input);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    ctx.send(file);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
    ctx.send(file);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull FileChannel file) {
    ctx.send(file);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull StatusCode statusCode) {
    ctx.send(statusCode);
    return this;
  }

  @Nonnull @Override public Context sendError(@Nonnull Throwable cause) {
    ctx.sendError(cause);
    return this;
  }

  @Nonnull @Override
  public Context sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode) {
    ctx.sendError(cause, statusCode);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return ctx.isResponseStarted();
  }

  @Override public boolean getResetHeadersOnError() {
    return ctx.getResetHeadersOnError();
  }

  @Override public Context setResetHeadersOnError(boolean value) {
    ctx.setResetHeadersOnError(value);
    return this;
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) throws RegistryException {
    return ctx.require(type);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name)
      throws RegistryException {
    return ctx.require(type, name);
  }

  @Nonnull @Override public <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    return ctx.require(key);
  }

  @Override public String toString() {
    return ctx.toString();
  }
}
