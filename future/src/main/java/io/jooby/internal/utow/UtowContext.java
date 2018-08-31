package io.jooby.internal.utow;

import io.jooby.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.*;
import io.undertow.util.*;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.jooby.funzy.Throwing.throwingConsumer;

public class UtowContext extends BaseContext {

  private final HttpServerExchange exchange;
  private final Executor executor;
  private final Path tmpdir;
  private final Route.RootErrorHandler errorHandler;
  private QueryString query;
  private Form form;
  private Multipart multipart;
  private List<Value.Upload> files;
  private Value.Object headers;

  public UtowContext(HttpServerExchange exchange, Executor executor,
      Route.RootErrorHandler errorHandler, Path tmpdir) {
    this.exchange = exchange;
    this.executor = executor;
    this.tmpdir = tmpdir;
    this.errorHandler = errorHandler;
  }

  @Nonnull @Override public Body body() {
    requireBlocking();
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
    return Body.of(exchange.getInputStream(), exchange.getResponseContentLength());
  }

  @Nonnull @Override public String method() {
    return exchange.getRequestMethod().toString().toUpperCase();
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
    requireBlocking();
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

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    exchange.dispatch(SameThreadExecutor.INSTANCE, action);
    return this;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    exchange.setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType, @Nullable String charset) {
    if (charset == null) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    } else {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType + ";charset=" + charset);
    }
    return this;
  }

  @Nonnull @Override public Context length(long length) {
    exchange.setResponseContentLength(length);
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    byte[] result = (byte[]) fireAfter(data);
    return sendBytes(ByteBuffer.wrap(result));
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    String result = (String) fireAfter(data);
    exchange.getResponseSender().send(result, charset);
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    ByteBuffer result = (ByteBuffer) fireAfter(data);
    exchange.getResponseSender().send(result);
    return this;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    exchange.setStatusCode(statusCode).endExchange();
    return this;
  }

  @Nonnull @Override public Context sendError(Throwable cause) {
    errorHandler.apply(this, cause);
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
