package io.jooby;

import io.jooby.internal.UrlParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class FakeContext implements Context {

  private String method = Router.GET;

  private Route route;

  private String pathString;

  private Map<String, String> pathMap;

  private QueryString query = UrlParser.queryString("?");

  private String queryString;

  private Value.Object headers = Value.headers();

  private Formdata formdata = new Formdata();

  private Multipart multipart = new Multipart();

  private Body body;

  private Map<String, Object> locals = new HashMap<>();

  private Map<String, Parser> parsers = new HashMap<>();

  private boolean ioThread;

  private Map<String, Object> responseHeaders = new HashMap<>();

  private long length;
  private String responseContentType;
  private String responseCharset;
  private int responseStatusCode;
  private Object result;
  private boolean responseStarted;

  @Nonnull @Override public String method() {
    return method;
  }

  public Context setMethod(String method) {
    this.method = method;
    return this;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  public FakeContext setRoute(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return pathString;
  }

  public FakeContext setPathString(String pathString) {
    this.pathString = pathString;
    return this;
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  public FakeContext setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Nonnull @Override public QueryString query() {
    return query;
  }

  @Nonnull @Override public String queryString() {
    return queryString;
  }

  public FakeContext setQueryString(String queryString) {
    this.queryString = queryString;
    this.query = UrlParser.queryString("?" + queryString);
    return this;
  }

  @Nonnull @Override public Value headers() {
    return headers;
  }

  public FakeContext setHeaders(Value.Object headers) {
    this.headers = headers;
    return this;
  }

  @Nonnull @Override public Formdata form() {
    return formdata;
  }

  public FakeContext setForm(Formdata formdata) {
    this.formdata = formdata;
    return this;
  }

  @Nonnull @Override public Multipart multipart() {
    return multipart;
  }

  public void setMultipart(Multipart multipart) {
    this.multipart = multipart;
  }

  @Nonnull @Override public Body body() {
    return body;
  }

  public FakeContext setBody(Body body) {
    this.body = body;
    return this;
  }

  @Nonnull @Override public Parser parser(@Nonnull String contentType) {
    return parsers.get(contentType);
  }

  @Nonnull @Override public FakeContext parser(@Nonnull String contentType, @Nonnull Parser parser) {
    parsers.put(contentType, parser);
    return this;
  }

  @Override public boolean isInIoThread() {
    return ioThread;
  }

  public FakeContext setIoThread(boolean ioThread) {
    this.ioThread = ioThread;
    return this;
  }

  @Nonnull @Override public FakeContext dispatch(@Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override
  public FakeContext dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override public FakeContext detach(@Nonnull Runnable action) {
    action.run();
    return null;
  }

  @Nullable @Override public <T> T get(String name) {
    return (T) locals.get(name);
  }

  @Nonnull @Override public FakeContext set(@Nonnull String name, @Nonnull Object value) {
    locals.put(name, value);
    return this;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public FakeContext header(@Nonnull String name, @Nonnull Date value) {
    Context.super.header(name, value);
    return this;
  }

  @Nonnull @Override public FakeContext header(@Nonnull String name, @Nonnull Instant value) {
    Context.super.header(name, value);
    return this;
  }

  @Nonnull @Override public FakeContext header(@Nonnull String name, @Nonnull Object value) {
    Context.super.header(name, value);
    return this;
  }

  @Nonnull @Override public FakeContext header(@Nonnull String name, @Nonnull String value) {
    responseHeaders.put(name, value);
    return this;
  }

  @Nonnull @Override public FakeContext length(long length) {
    this.length = length;
    return this;
  }

  public long getResponseLength() {
    return length;
  }

  @Nonnull @Override public FakeContext type(@Nonnull String contentType, @Nullable String charset) {
    this.responseContentType = contentType;
    this.responseCharset = charset;
    return this;
  }

  @Nonnull @Override public FakeContext statusCode(int statusCode) {
    this.responseStatusCode = statusCode;
    return this;
  }

  @Nonnull @Override public FakeContext send(@Nonnull Object result) {
    this.result = result;
    return this;
  }

  public Object getResult() {
    return result;
  }

  public String getResultText() {
    return (String) result;
  }

  @Nonnull @Override public FakeContext sendText(@Nonnull String data) {
    responseStarted = true;
    result = data;
    return this;
  }

  @Nonnull @Override public FakeContext sendText(@Nonnull String data, @Nonnull Charset charset) {
    responseStarted = true;
    result = data;
    return this;
  }

  @Nonnull @Override public FakeContext sendBytes(@Nonnull byte[] data) {
    responseStarted = true;
    result = data;
    return this;
  }

  @Nonnull @Override public FakeContext sendBytes(@Nonnull ByteBuffer data) {
    result = data;
    return this;
  }

  @Nonnull @Override public FakeContext sendStatusCode(int statusCode) {
    responseStarted = true;
    result = statusCode;
    responseStatusCode =statusCode;
    return this;
  }

  @Nonnull @Override public FakeContext sendError(@Nonnull Throwable cause) {
    responseStarted = true;
    result = cause;
    return this;
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override public void destroy() {

  }

  @Override public String name() {
    return "fake";
  }
}
