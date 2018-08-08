package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Route;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class UtowContext implements Context {

  private final Route route;
  private final HttpServerExchange exchange;
  private final Executor executor;
  private final Map<String, Object> locals = new HashMap<>();

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

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public Route.Filter gzip() {
    return next -> ctx -> {
      if (exchange.getRequestHeaders().contains(Headers.ACCEPT_ENCODING)) {
        AtomicReference<Object> holder = new AtomicReference<>();
        new EncodingHandler.Builder().build(null)
            .wrap(ex -> {
              try {
                holder.set(next.apply(ctx));
              } catch (Throwable x) {
                holder.set(x);
              }
            })
            .handleRequest(exchange);
        Object value = holder.get();
        if (value instanceof Exception) {
          throw (Exception) value;
        }
        return value;
      } else {
        // Ignore gzip, move to next:
        return next.apply(ctx);
      }
    };
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
