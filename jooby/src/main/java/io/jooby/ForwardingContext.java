package io.jooby;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
  private final Context context;

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ForwardingContext(@Nonnull Context context) {
    this.context = context;
  }

  @Override @Nonnull public Map<String, Object> getAttributes() {
    return context.getAttributes();
  }

  @Nonnull @Override public <T> T attribute(@Nonnull String key) {
    return context.attribute(key);
  }

  @Nonnull @Override public Context attribute(@Nonnull String key, Object value) {
    context.attribute(key, value);
    return this;
  }

  @Override @Nonnull public Router getRouter() {
    return context.getRouter();
  }

  @Nonnull @Override public FlashMap flashMap() {
    return context.flashMap();
  }

  @Nonnull @Override public Value flash(@Nonnull String name) {
    return context.flash(name);
  }

  @Nonnull @Override public Context flash(@Nonnull String name, @Nonnull String value) {
    return context.flash(name, value);
  }

  @Nonnull @Override public Session session() {
    return context.session();
  }

  @Nullable @Override public Session sessionOrNull() {
    return context.sessionOrNull();
  }

  @Nonnull @Override public Value cookie(@Nonnull String name) {
    return context.cookie(name);
  }

  @Override @Nonnull public Map<String, String> cookieMap() {
    return context.cookieMap();
  }

  @Override @Nonnull public String getMethod() {
    return context.getMethod();
  }

  @Override @Nonnull public Route getRoute() {
    return context.getRoute();
  }

  @Override @Nonnull public Context setRoute(@Nonnull Route route) {
    return context.setRoute(route);
  }

  @Override @Nonnull public String pathString() {
    return context.pathString();
  }

  @Nonnull @Override public Value path(@Nonnull String name) {
    return context.path(name);
  }

  @Nonnull @Override public <T> T path(@Nonnull Reified<T> type) {
    return context.path(type);
  }

  @Nonnull @Override public <T> T path(@Nonnull Class<T> type) {
    return context.path(type);
  }

  @Nonnull @Override public Value path() {
    return context.path();
  }

  @Override @Nonnull public Map<String, String> pathMap() {
    return context.pathMap();
  }

  @Override @Nonnull public Context setPathMap(@Nonnull Map<String, String> pathMap) {
    context.setPathMap(pathMap);
    return this;
  }

  @Override @Nonnull public QueryString query() {
    return context.query();
  }

  @Nonnull @Override public Value query(@Nonnull String name) {
    return context.query(name);
  }

  @Nonnull @Override public String queryString() {
    return context.queryString();
  }

  @Nonnull @Override public <T> T query(@Nonnull Reified<T> type) {
    return context.query(type);
  }

  @Nonnull @Override public <T> T query(@Nonnull Class<T> type) {
    return context.query(type);
  }

  @Nonnull @Override public Map<String, String> queryMap() {
    return context.queryMap();
  }

  @Nonnull @Override public Map<String, List<String>> queryMultimap() {
    return context.queryMultimap();
  }

  @Override @Nonnull public Value headers() {
    return context.headers();
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return context.header(name);
  }

  @Nonnull @Override public Map<String, String> headerMap() {
    return context.headerMap();
  }

  @Nonnull @Override public Map<String, List<String>> headerMultimap() {
    return context.headerMultimap();
  }

  @Override public boolean accept(@Nonnull MediaType contentType) {
    return context.accept(contentType);
  }

  @Nullable @Override public MediaType accept(@Nonnull List<MediaType> produceTypes) {
    return context.accept(produceTypes);
  }

  @Nullable @Override public MediaType getRequestType() {
    return context.getRequestType();
  }

  @Nonnull @Override public MediaType getRequestType(MediaType defaults) {
    return context.getRequestType(defaults);
  }

  @Override public long getRequestLength() {
    return context.getRequestLength();
  }

  @Override @Nonnull public String getRemoteAddress() {
    return context.getRemoteAddress();
  }

  @Nonnull @Override public String getHost() {
    return context.getHost();
  }

  @Override @Nonnull public String getProtocol() {
    return context.getProtocol();
  }

  @Override @Nonnull public String getScheme() {
    return context.getScheme();
  }

  @Override @Nonnull public Formdata form() {
    return context.form();
  }

  @Nonnull @Override public Map<String, List<String>> formMultimap() {
    return context.formMultimap();
  }

  @Nonnull @Override public Map<String, String> formMap() {
    return context.formMap();
  }

  @Nonnull @Override public Value form(@Nonnull String name) {
    return context.form(name);
  }

  @Nonnull @Override public <T> T form(@Nonnull Reified<T> type) {
    return context.form(type);
  }

  @Nonnull @Override public <T> T form(@Nonnull Class<T> type) {
    return context.form(type);
  }

  @Override @Nonnull public Multipart multipart() {
    return context.multipart();
  }

  @Nonnull @Override public Value multipart(@Nonnull String name) {
    return context.multipart(name);
  }

  @Nonnull @Override public <T> T multipart(@Nonnull Reified<T> type) {
    return context.multipart(type);
  }

  @Nonnull @Override public <T> T multipart(@Nonnull Class<T> type) {
    return context.multipart(type);
  }

  @Nonnull @Override public Map<String, List<String>> multipartMultimap() {
    return context.multipartMultimap();
  }

  @Nonnull @Override public Map<String, String> multipartMap() {
    return context.multipartMap();
  }

  @Nonnull @Override public List<FileUpload> files() {
    return context.files();
  }

  @Nonnull @Override public List<FileUpload> files(@Nonnull String name) {
    return context.files(name);
  }

  @Nonnull @Override public FileUpload file(@Nonnull String name) {
    return context.file(name);
  }

  @Override @Nonnull public Body body() {
    return context.body();
  }

  @Nonnull @Override public <T> T body(@Nonnull Reified<T> type) {
    return context.body(type);
  }

  @Nonnull @Override public <T> T body(@Nonnull Reified<T> type, @Nonnull MediaType contentType) {
    return context.body(type, contentType);
  }

  @Nonnull @Override public <T> T body(@Nonnull Class type) {
    return context.body(type);
  }

  @Nonnull @Override public <T> T body(@Nonnull Class type, @Nonnull MediaType contentType) {
    return context.body(type, contentType);
  }

  @Nonnull @Override public MessageDecoder decoder(@Nonnull MediaType contentType) {
    return context.decoder(contentType);
  }

  @Override public boolean isInIoThread() {
    return context.isInIoThread();
  }

  @Override @Nonnull public Context dispatch(@Nonnull Runnable action) {
    context.dispatch(action);
    return this;
  }

  @Override @Nonnull public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    context.dispatch(executor, action);
    return this;
  }

  @Override @Nonnull public Context detach(@Nonnull Runnable action) {
    context.detach(action);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    context.setResponseHeader(name, value);
    return this;
  }

  @Nonnull @Override
  public Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    context.setResponseHeader(name, value);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    context.setResponseHeader(name, value);
    return this;
  }

  @Override @Nonnull public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    context.setResponseHeader(name, value);
    return this;
  }

  @Override @Nonnull public Context removeResponseHeader(@Nonnull String name) {
    context.removeResponseHeader(name);
    return this;
  }

  @Override @Nonnull public Context setResponseLength(long length) {
    context.setResponseLength(length);
    return this;
  }

  @Override @Nonnull public Context setResponseCookie(@Nonnull Cookie cookie) {
    context.setResponseCookie(cookie);
    return this;
  }

  @Override @Nonnull public Context setResponseType(@Nonnull String contentType) {
    context.setResponseType(contentType);
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull MediaType contentType) {
    context.setResponseType(contentType);
    return this;
  }

  @Override @Nonnull public Context setResponseType(@Nonnull MediaType contentType,
      @Nullable Charset charset) {
    context.setResponseType(contentType, charset);
    return this;
  }

  @Override @Nonnull public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    context.setResponseType(contentType);
    return this;
  }

  @Override @Nonnull public MediaType getResponseType() {
    return context.getResponseType();
  }

  @Nonnull @Override public Context setResponseCode(@Nonnull StatusCode statusCode) {
    context.setResponseCode(statusCode);
    return this;
  }

  @Override @Nonnull public Context setResponseCode(int statusCode) {
    context.setResponseCode(statusCode);
    return this;
  }

  @Override @Nonnull public StatusCode getResponseCode() {
    return context.getResponseCode();
  }

  @Nonnull @Override public Context render(@Nonnull Object value) {
    context.render(value);
    return this;
  }

  @Override @Nonnull public OutputStream responseStream() {
    return context.responseStream();
  }

  @Nonnull @Override public OutputStream responseStream(@Nonnull MediaType contentType) {
    return context.responseStream(contentType);
  }

  @Nonnull @Override public Context responseStream(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    return context.responseStream(contentType, consumer);
  }

  @Nonnull @Override
  public Context responseStream(@Nonnull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    return context.responseStream(consumer);
  }

  @Override @Nonnull public Sender responseSender() {
    return context.responseSender();
  }

  @Nonnull @Override public PrintWriter responseWriter() {
    return context.responseWriter();
  }

  @Nonnull @Override public PrintWriter responseWriter(@Nonnull MediaType contentType) {
    return context.responseWriter(contentType);
  }

  @Override @Nonnull public PrintWriter responseWriter(@Nonnull MediaType contentType,
      @Nullable Charset charset) {
    return context.responseWriter(contentType, charset);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    return context.responseWriter(consumer);
  }

  @Nonnull @Override public Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return context.responseWriter(contentType, consumer);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    return context.responseWriter(contentType, charset, consumer);
  }

  @Nonnull @Override public Context sendRedirect(@Nonnull String location) {
    context.sendRedirect(location);
    return this;
  }

  @Nonnull @Override
  public Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location) {
    context.sendRedirect(redirect, location);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    context.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull String data, @Nonnull Charset charset) {
    context.send(data, charset);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull byte[] data) {
    context.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull ByteBuffer data) {
    context.send(data);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuf data) {
    context.send(data);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull ReadableByteChannel channel) {
    context.send(channel);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull InputStream input) {
    context.send(input);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    context.send(file);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
    context.send(file);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull FileChannel file) {
    context.send(file);
    return this;
  }

  @Override @Nonnull public Context send(@Nonnull StatusCode statusCode) {
    context.send(statusCode);
    return this;
  }

  @Nonnull @Override public Context sendError(@Nonnull Throwable cause) {
    context.sendError(cause);
    return this;
  }

  @Nonnull @Override
  public Context sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode) {
    context.sendError(cause, statusCode);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return context.isResponseStarted();
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type) throws RegistryException {
    return context.require(type);
  }

  @Nonnull @Override public <T> T require(@Nonnull Class<T> type, @Nonnull String name)
      throws RegistryException {
    return context.require(type, name);
  }

  @Nonnull @Override public <T> T require(@Nonnull ServiceKey<T> key) throws RegistryException {
    return context.require(key);
  }

  @Override public String toString() {
    return context.toString();
  }
}
