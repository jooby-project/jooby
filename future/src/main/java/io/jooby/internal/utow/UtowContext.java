package io.jooby.internal.utow;

import io.jooby.*;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.form.*;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.jooby.funzy.Throwing.throwingConsumer;

public class UtowContext implements Context {

  private final Route route;
  private final HttpServerExchange exchange;
  private final Executor executor;
  private final Map<String, Object> locals = new HashMap<>();
  private final Path tmpdir;
  private QueryString query;
  private Form form;
  private Multipart multipart;
  private List<Value.Upload> files;
  private Value.Object headers;
  private Map<String, Parser> parsers = new HashMap<>();

  public UtowContext(HttpServerExchange exchange, Executor executor, Route route, Path tmpdir) {
    this.exchange = exchange;
    this.executor = executor;
    this.route = route;
    this.tmpdir = tmpdir;
  }

  @Nonnull @Override public Parser parser(@Nonnull String contentType) {
    return parsers.getOrDefault(contentType, Parser.NOT_ACCEPTABLE);
  }

  @Nonnull @Override public Context parser(@Nonnull String contentType, @Nonnull Parser parser) {
    parsers.put(contentType, parser);
    return this;
  }

  @Nonnull @Override public Body body() {
    if (isInIoThread()) {
      throw new IllegalStateException(
          "Attempted to do blocking IO from the IO thread. This is prohibited as it may result in deadlocks");
    }
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
    return Body.of(exchange.getInputStream(), exchange.getResponseContentLength());
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

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(name, exchange.getRequestHeaders().get(name));
  }

  @Nonnull @Override public Value headers() {
    HeaderMap map = exchange.getRequestHeaders();
    if (headers == null) {
      headers = Value.headers();
      Collection<HttpString> names = map.getHeaderNames();
      for (HttpString name : names) {
        HeaderValues values = map.get(name);
        headers.put(name.toString(), values);
      }
    }
    return headers;
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

  @Nonnull @Override public Multipart multipart() {
    if (isInIoThread()) {
      throw new IllegalStateException(
          "Attempted to do blocking IO from the IO thread. This is prohibited as it may result in deadlocks");
    }
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      if (!exchange.isBlocking()) {
        exchange.startBlocking();
      }
      try (FormDataParser parser = new MultiPartParserDefinition()
          .setDefaultEncoding(StandardCharsets.UTF_8.name())
          .setTempFileLocation(tmpdir)
          .create(exchange)) {
        formData(multipart, parser.parseBlocking());
      } catch (Exception x) {
        throw Throwing.sneakyThrow(x);
      }
    }
    return multipart;
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

  @Override public void destroy() {
    if (files != null) {
      // TODO: use a log
      files.forEach(throwingConsumer(Value.Upload::destroy).onFailure(x -> x.printStackTrace()));
    }
  }

  private Value.Upload register(Value.Upload upload) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(upload);
    return upload;
  }

  private void formData(Form form, FormData data) {
    Iterator<String> it = data.iterator();
    while (it.hasNext()) {
      String path = it.next();
      Deque<FormData.FormValue> values = data.get(path);
      for (FormData.FormValue value : values) {
        if (value.isFile()) {
          form.put(path, register(new UtowUpload(path, value)));
        } else {
          form.put(path, value.getValue());
        }
      }
    }
  }
}
