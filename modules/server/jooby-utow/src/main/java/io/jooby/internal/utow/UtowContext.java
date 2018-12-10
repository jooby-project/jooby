/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.*;
import io.undertow.util.*;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;

import static io.jooby.Throwing.throwingConsumer;

public class UtowContext extends BaseContext {

  private final HttpServerExchange exchange;
  private final Path tmpdir;
  private final Route.RootErrorHandler errorHandler;
  private final Executor worker;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<Upload> files;
  private Value.Object headers;

  public UtowContext(HttpServerExchange exchange, Executor worker,
      Route.RootErrorHandler errorHandler, Path tmpdir) {
    this.exchange = exchange;
    this.worker = worker;
    this.tmpdir = tmpdir;
    this.errorHandler = errorHandler;
  }

  @Override public String name() {
    return "utow";
  }

  @Nonnull @Override public Body body() {
    requireBlocking();
    if (!exchange.isBlocking()) {
      exchange.startBlocking();
    }
    return Body.of(exchange.getInputStream(), exchange.getRequestContentLength());
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

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = new Formdata();
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

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(worker, action);
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

  @Nonnull @Override public Context header(@Nonnull String name, @Nonnull String value) {
    exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
    return this;
  }

  @Nonnull @Override public Context type(@Nonnull String contentType, @Nullable String charset) {
    if (charset == null) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    } else {
      exchange.getResponseHeaders()
          .put(Headers.CONTENT_TYPE, contentType + ";charset=" + charset);
    }
    return this;
  }

  @Nonnull @Override public Context length(long length) {
    exchange.setResponseContentLength(length);
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull byte[] data) {
    return sendBytes(ByteBuffer.wrap(data));
  }

  @Nonnull @Override public Context sendText(@Nonnull String data, @Nonnull Charset charset) {
    exchange.getResponseSender().send(data, charset);
    return this;
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuffer data) {
    exchange.getResponseSender().send(data);
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
      files.forEach(throwingConsumer(Upload::destroy));
    }
  }

  private Upload register(Upload upload) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(upload);
    return upload;
  }

  private void formData(Formdata form, FormData data) {
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
