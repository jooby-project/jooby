package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

// TODO: WIP
public class FakeContext implements Context {
  private Route route;
  private String method = Router.GET;
  private String path = "/";
  private Map<String, String> params = Collections.emptyMap();

  @Nonnull @Override public String method() {
    return method;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  public FakeContext route(Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String path() {
    return path;
  }

  public FakeContext path(String path) {
    this.path = path;
    return this;
  }

  @Nonnull @Override public Map<String, String> params() {
    return params;
  }

  @Nonnull @Override public QueryString query() {
    return null;
  }

  @Nonnull @Override public Value headers() {
    return null;
  }

  @Nonnull @Override public Formdata form() {
    return null;
  }

  @Nonnull @Override public Multipart multipart() {
    return null;
  }

  @Nonnull @Override public Body body() {
    return null;
  }

  @Nonnull @Override public Parser parser(@Nonnull String contentType) {
    return null;
  }

  @Nonnull @Override public Context parser(@Nonnull String contentType, @Nonnull Parser parser) {
    return null;
  }

  @Override public boolean isInIoThread() {
    return false;
  }

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return null;
  }

  @Nonnull @Override public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    return null;
  }

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    return null;
  }

  @Nullable @Override public <T> T get(String name) {
    return null;
  }

  @Nonnull @Override public Context set(@Nonnull String name, @Nonnull Object value) {
    return null;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return null;
  }

  @Nonnull @Override public Context header(@Nonnull String name, @Nonnull String value) {
    return null;
  }

  @Nonnull @Override public Context length(long length) {
    return null;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType, @Nullable String charset) {
    return null;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    return null;
  }

  @Nonnull @Override public Context send(@Nonnull Object result) {
    return null;
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    return null;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    return null;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    return null;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    return null;
  }

  @Nonnull @Override public Context sendError(@Nonnull Throwable cause) {
    return null;
  }

  @Override public boolean isResponseStarted() {
    return false;
  }

  @Override public void destroy() {

  }

  @Override public String name() {
    return null;
  }
}
