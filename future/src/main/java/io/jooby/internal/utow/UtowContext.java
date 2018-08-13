package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.Form;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Value;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.util.Headers;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class UtowContext implements Context {

  private final Route route;
  private final HttpServerExchange exchange;
  private final Executor executor;
  private final Map<String, Object> locals = new HashMap<>();
  private QueryString query;
  private Form form;

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

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      String queryString = exchange.getQueryString();
      if (queryString.length() == 0) {
        return QueryString.EMPTY;
      }
      query = Value.queryString('?' + queryString);
    }
    return query;
  }

  @Nonnull @Override public Form form() {
    if (form == null) {
      form = new Form();
      try (FormDataParser parser = new FormEncodedDataDefinition()
          .setDefaultEncoding(StandardCharsets.UTF_8.name())
          .create(exchange)) {
        formData(form, parser.parseBlocking());
      } catch (Exception x) {
        throw Throwing.sneakyThrow(x);
      }
    }
    return form;
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

  private void formData(Form form, FormData data) {
    Iterator<String> it = data.iterator();
    while (it.hasNext()) {
      String path = it.next();
      Deque<FormData.FormValue> values = data.get(path);
      if (values.size() == 1) {
        form.put(path, values.getFirst().getValue());
      } else {
        form.put(path, values.stream()
            .map(FormData.FormValue::getValue)
            .collect(Collectors.toList()));
      }
    }
  }
}
