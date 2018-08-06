package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Handler;
import io.jooby.Route;
import io.undertow.server.HttpServerExchange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;

public class UtowContext implements Context {

  private final Route route;
  private final HttpServerExchange exchange;
  private final Executor executor;

  public UtowContext(HttpServerExchange exchange, Executor executor, Route route) {
    this.exchange = exchange;
    this.executor = executor;
    this.route = route;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public String path() {
    return exchange.getRequestPath();
  }

  @Override public boolean isInIoThread() {
    return exchange.isInIoThread();
  }

  @Nonnull @Override public Executor worker() {
    return executor;
  }

  @Nonnull @Override public Context dispatch(@Nonnull Executor executor,
      @Nonnull Runnable action) {
    exchange.dispatch(executor, action);
    return this;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType) {
    return null;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    exchange.getResponseSender().send(data, StandardCharsets.UTF_8);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    exchange.getResponseSender().send(data, charset);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    exchange.getResponseSender().send(data);
    return this;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    exchange.setStatusCode(statusCode).endExchange();
    return this;
  }

  @Override public boolean isResponseStarted() {
    return exchange.isResponseStarted();
  }
}
