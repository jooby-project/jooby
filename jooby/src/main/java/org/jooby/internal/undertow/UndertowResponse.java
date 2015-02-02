/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.undertow;

import static java.util.Objects.requireNonNull;
import io.undertow.io.UndertowOutputStream;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooby.Body;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.fn.ExSupplier;
import org.jooby.internal.BodyConverterSelector;
import org.jooby.internal.BodyWriterImpl;
import org.jooby.internal.BuiltinBodyConverter;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.SetHeaderImpl;
import org.jooby.internal.reqparam.RootParamConverter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

public class UndertowResponse implements Response {

  private HttpServerExchange exchange;

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private Status status;

  private MediaType type;

  private Route route;

  private Map<String, Object> locals;

  private SetHeaderImpl setHeader;

  private Optional<String> referer;

  public UndertowResponse(final HttpServerExchange exchange,
      final Injector injector,
      final Route route,
      final Map<String, Object> locals,
      final BodyConverterSelector selector,
      final Charset charset,
      final Optional<String> referer) {
    this.exchange = requireNonNull(exchange, "An exchange is required.");
    this.injector = requireNonNull(injector, "An injector is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "The locals is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.referer = requireNonNull(referer, "A referer is required.");

    this.setHeader = new SetHeaderImpl(
        (name, value) -> exchange.getResponseHeaders().put(new HttpString(name), value));
  }

  @Override
  public void download(final String filename, final InputStream stream) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(stream, "A stream is required.");

    download(filename, stream, BuiltinBodyConverter.formatStream);
  }

  @Override
  public void download(final String filename, final Reader reader) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(reader, "A reader is required.");

    download(filename, reader, BuiltinBodyConverter.formatReader);
  }

  @Override
  public Response cookie(final Cookie cookie) {
    requireNonNull(cookie, "A cookie is required.");
    exchange.getResponseCookies()
      .put(cookie.name(), toUndertowCookie(cookie));

    return this;
  }

  @Override
  public Response clearCookie(final String name) {
    requireNonNull(name, "A cookie's name is required.");
    // it was set in the current req/rsp call?
    exchange.getResponseCookies().remove(name);

    // or clear existing cookie
    Optional.ofNullable(exchange.getRequestCookies().get(name)).ifPresent(cookie -> {
      cookie.setMaxAge(0);
      exchange.getResponseCookies().put(name, cookie);
    });
    return this;
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "A header's name is required.");
    HeaderValues headers = exchange.getResponseHeaders().get(name);
    return new MutantImpl(injector.getInstance(RootParamConverter.class), headers);
  }

  @Override
  public Response header(final String name, final char value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final byte value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final short value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final int value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final long value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final float value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final double value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final CharSequence value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final Date value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Response charset(final Charset charset) {
    this.charset = requireNonNull(charset, "A charset is required.");
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,
        type().orElse(MediaType.html).name() + ";charset=" + charset.name());
    return this;
  }

  @Override
  public Response length(final long length) {
    exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, length);
    return this;
  }

  @Override
  public Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  @Override
  public Response type(final MediaType type) {
    this.type = requireNonNull(type, "Content-Type is required.");
    String charset = type.isText() ? ";charset=" + this.charset.name() : "";
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, type.name() + charset);
    return this;
  }

  @Override
  public void send(final Body body) throws Exception {
    requireNonNull(body, "A body is required.");
    Optional<Object> content = body.content();
    Body.Formatter converter = content.isPresent()
        ? selector.formatter(content.get(), route.produces())
            .orElseThrow(() -> new Err(Status.NOT_ACCEPTABLE))
        : noop(route.produces());
    send(body, converter);
  }

  @Override
  public Formatter format() {
    final Map<MediaType, ExSupplier<Object>> strategies = new LinkedHashMap<>();
    List<MediaType> types = new LinkedList<>();

    return new Formatter() {

      @Override
      public Formatter when(final MediaType type, final ExSupplier<Object> supplier) {
        requireNonNull(type, "A media type is required.");
        requireNonNull(supplier, "A supplier fn is required.");
        strategies.put(type, supplier);
        types.add(type);
        return this;
      }

      @Override
      public void send() throws Exception {
        List<MediaType> produces = route.produces();

        ExSupplier<Object> provider = MediaType
            .matcher(produces)
            .first(types)
            .map(it -> strategies.get(it))
            .orElseThrow(
                () -> new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces))
            );

        Object result = provider.get();
        if (result instanceof Exception) {
          throw (Exception) result;
        }
        UndertowResponse.this.send(result);
      }

    };
  }

  @Override
  public void redirect(final Status status, final String location) throws Exception {
    requireNonNull(status, "A status is required.");
    requireNonNull(location, "A location is required.");

    send(Body.body(status).header("Location", location));
  }

  @Override
  public Optional<Status> status() {
    return Optional.ofNullable(status);
  }

  @Override
  public Response status(final Status status) {
    this.status = requireNonNull(status, "A status is required.");
    exchange.setResponseCode(status.value());
    return this;
  }

  @Override
  public boolean committed() {
    return exchange.isResponseStarted();
  }

  public void route(final Route route) {
    this.route = route;
  }

  public void reset() {
    ((UndertowOutputStream) exchange.getOutputStream()).resetBuffer();
    status = null;
    exchange.getResponseHeaders().clear();
  }

  public List<MediaType> viewableTypes() {
    return selector.viewableTypes();
  }

  @Override
  public void end() {
    if (!committed()) {
      if (status == null) {
        status(200);
      }
    }
    // this is a noop when response has been set, still call it...
    exchange.endExchange();
  }

  public void send(final Body body, final Body.Formatter formatter) throws Exception {
    requireNonNull(body, "A response message is required.");
    requireNonNull(formatter, "A converter is required.");

    type(body.type().orElseGet(() -> type().orElseGet(() -> formatter.types().get(0))));

    status(body.status().orElseGet(() -> status().orElseGet(() -> Status.OK)));

    Runnable setHeaders = () -> body.headers().forEach(
        (name, value) -> {
          // reset back header while doing a redirect
          if ("location".equalsIgnoreCase(name) && "back".equalsIgnoreCase(value)
              && status().map(s -> s.value() >= 300 && s.value() < 400).orElse(false)) {
            header(name, referer.orElse("/"));
          } else {
            header(name, value);
          }
        });

    // byte version of http body
    ExSupplier<OutputStream> stream = () -> {
      setHeaders.run();
      return exchange.getOutputStream();
    };

    // text version of http body
    ExSupplier<Writer> writer = () -> {
      charset(charset);
      setHeaders.run();
      return new OutputStreamWriter(exchange.getOutputStream(), charset);
    };

    Optional<Object> content = body.content();
    if (content.isPresent()) {
      Object message = content.get();
      if (message instanceof Status) {
        // override status when message is a status
        status((Status) message);
      }

      formatter.format(message, new BodyWriterImpl(charset, ImmutableMap.copyOf(locals),
          stream, writer));
    } else {
      // noop, but we apply headers.
      stream.get().close();
    }
    // end response
    end();
  }

  private io.undertow.server.handlers.Cookie toUndertowCookie(final Cookie cookie) {
    io.undertow.server.handlers.Cookie result =
        new CookieImpl(cookie.name(), cookie.value().orElse(null));

    cookie.comment().ifPresent(result::setComment);
    cookie.domain().ifPresent(result::setDomain);
    result.setHttpOnly(cookie.httpOnly());
    result.setMaxAge((int)cookie.maxAge());
    result.setPath(cookie.path());
    result.setSecure(cookie.secure());
    result.setVersion(cookie.version());

    return result;
  }

  private void download(final String filename, final Object in,
      final Body.Formatter formatter) throws Exception {

    contentDisposition(filename);

    Body body = Body.body(in);
    body.type(type().orElseGet(() -> MediaType.byPath(filename).orElse(MediaType.octetstream)));

    send(body, formatter);
  }

  private void contentDisposition(final String filename) {
    String basename = filename;
    int last = filename.lastIndexOf('/');
    if (last >= 0) {
      basename = basename.substring(last + 1);
    }
    header("Content-Disposition", "attachment; filename=" + basename);
    header("Transfer-Encoding", "chunked");
  }

  private static Body.Formatter noop(final List<MediaType> types) {
    return new Body.Formatter() {

      @Override
      public void format(final Object body, final Body.Writer writer) throws Exception {
        writer.bytes(out -> out.close());
      }

      @Override
      public List<MediaType> types() {
        return types;
      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return true;
      }

    };
  }

}
