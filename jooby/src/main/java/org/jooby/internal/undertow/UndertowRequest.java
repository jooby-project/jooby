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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.HeaderMap;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jooby.Body.Parser;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.fn.Collectors;
import org.jooby.internal.BodyConverterSelector;
import org.jooby.internal.BodyReaderImpl;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.RequestScopedSession;
import org.jooby.internal.SessionManager;
import org.jooby.internal.reqparam.BeanParamInjector;
import org.jooby.internal.reqparam.RootParamConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;

public class UndertowRequest implements Request {

  interface ExchangeFn<T> {
    T get(HttpServerExchange exchange) throws IOException;
  }

  private static class MemoizingExchangeFn<T> implements ExchangeFn<T> {

    private ExchangeFn<T> fn;

    private T value;

    private T defaultValue;

    public MemoizingExchangeFn(final ExchangeFn<T> fn, final T defaultValue) {
      this.fn = fn;
      this.defaultValue = defaultValue;
    }

    @Override
    public T get(final HttpServerExchange exchange) throws IOException {
      if (value == null) {
        T form = fn.get(exchange);
        this.value = form == null ? defaultValue : form;
      }
      return this.value;
    }

  }

  private HttpServerExchange exchange;

  private Injector injector;

  // TODO: make route abstract? or throw UnsupportedException
  private Route route;

  private Map<String, Object> locals;

  private BodyConverterSelector selector;

  private MediaType type;

  private List<MediaType> accept;

  private Charset charset;

  private Locale locale;

  private Session session;

  private final ExchangeFn<FormData> formParser;

  private RootParamConverter converter;

  private Map<String, Mutant> params = new HashMap<>();

  public UndertowRequest(final HttpServerExchange exchange,
      final Injector injector,
      final Route route,
      final Map<String, Object> locals,
      final BodyConverterSelector selector,
      final MediaType contentType,
      final List<MediaType> accept,
      final Charset charset,
      final Locale locale) {
    this.exchange = requireNonNull(exchange, "An exchange is required.");
    this.injector = requireNonNull(injector, "An injector is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "The locals is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.type = requireNonNull(contentType, "A contentType is required.");
    this.accept = requireNonNull(accept, "An accept header is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.locale = requireNonNull(locale, "A locale is required.");
    formParser = formParser(contentType, injector.getInstance(Config.class));
    converter = injector.getInstance(RootParamConverter.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.get(name));
  }

  @Override
  public Map<String, Object> attributes() {
    return ImmutableMap.copyOf(locals);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> unset(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.remove(name));
  }

  @Override
  public MediaType type() {
    return type;
  }

  @Override
  public List<MediaType> accept() {
    return accept;
  }

  @Override
  public Optional<MediaType> accepts(final List<MediaType> types) {
    requireNonNull(types, "Media types are required.");
    return MediaType.matcher(accept).first(types);
  }

  @Override
  public Map<String, Mutant> params() throws Exception {
    List<String> names = new ArrayList<>();
    // path vars
    names.addAll(route.vars().keySet());
    // query params
    names.addAll(exchange.getQueryParameters().keySet());

    // post params
    FormData form = formParser.get(exchange);
    form.forEach(name -> names.add(name));

    Map<String, Mutant> params = new LinkedHashMap<>();
    for (String name : names) {
      params.put(name, param(name));
    }
    return params;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T params(final Class<T> beanType) throws Exception {
    return (T) BeanParamInjector.createAndInject(this, beanType);
  }

  @Override
  public Mutant param(final String name) throws Exception {
    requireNonNull(name, "Parameter's name is missing.");
    Mutant param = this.params.get(name);
    if (param == null) {
      String pathparam = route.vars().get(name);
      Builder<Object> builder = ImmutableList.builder();
      // path params
      if (pathparam != null) {
        builder.add(pathparam);
      }
      // query params
      Deque<String> query = exchange.getQueryParameters().get(name);
      if (query != null) {
        query.forEach(builder::add);
      }
      // form params
      FormData form = formParser.get(exchange);
      Optional.ofNullable(form.get(name)).ifPresent(values -> {
        values.forEach(value -> {
          if (value.isFile()) {
            builder.add(new UndertowUpload(injector, value));
          } else {
            builder.add(value.getValue());
          }
        });
      });
      param = new MutantImpl(converter, builder.build());
      this.params.put(name, param);
    }
    return param;
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "Header's name is missing.");
    return new MutantImpl(converter, exchange.getRequestHeaders().get(name));
  }

  @Override
  public Map<String, Mutant> headers() {
    Map<String, Mutant> result = new LinkedHashMap<>();
    HeaderMap headers = exchange.getRequestHeaders();
    headers.getHeaderNames().forEach(name ->
      result.put(name.toString(), new MutantImpl(converter, headers.get(name)))
    );
    return result;
  }

  @Override
  public Optional<Cookie> cookie(final String name) {
    requireNonNull(name, "Cookie's name is missing.");
    Map<String, io.undertow.server.handlers.Cookie> cookies = exchange.getRequestCookies();
    io.undertow.server.handlers.Cookie cookie = cookies.get(name);
    if (cookie == null) {
      return Optional.empty();
    }

    return Optional.of(cookie(cookie));
  }

  @Override
  public List<Cookie> cookies() {
    return exchange.getRequestCookies().values().stream()
        .map(UndertowRequest::cookie)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    if (length() > 0) {
      Optional<Parser> parser = selector.parser(type, ImmutableList.of(this.type));
      if (parser.isPresent()) {
        return parser.get().parse(type, new BodyReaderImpl(charset,
            () -> exchange.getInputStream()));
      }
      if (MediaType.form.matches(type()) || MediaType.multipart.matches(type())) {
        return (T) BeanParamInjector.createAndInject(this, type.getRawType());
      }
      throw new Err(Status.UNSUPPORTED_MEDIA_TYPE);
    }
    throw new Err(Status.BAD_REQUEST, "no body");
  }

  @Override
  public <T> T require(final Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Locale locale() {
    return locale;
  }

  @Override
  public long length() {
    return exchange.getRequestContentLength();
  }

  @Override
  public String ip() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostAddress();
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public String hostname() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostName();
  }

  @Override
  public Session session() {
    return ifSession().orElseGet(() -> {
      SessionManager sm = require(SessionManager.class);
      Session localSession = sm.get(this);
      Response rsp = require(Response.class);
      if (localSession == null) {
        localSession = sm.create(this, rsp);
      }
      this.session = new RequestScopedSession(sm, rsp, localSession, () -> this.session = null);
      return this.session;
    });
  }

  @Override
  public Optional<Session> ifSession() {
    return Optional.ofNullable(this.session);
  }

  @Override
  public String protocol() {
    return exchange.getProtocol().toString();
  }

  @Override
  public boolean secure() {
    return exchange.getRequestScheme().equalsIgnoreCase("https");
  }

  @Override
  public Request set(final String name, final Object value) {
    requireNonNull(name, "A local's name is required.");
    requireNonNull(value, "A local's value is required.");
    locals.put(name, value);
    return this;
  }

  @Override
  public Request unset() {
    locals.clear();
    return this;
  }

  public void route(final Route route) {
    this.route = requireNonNull(route, "A route is required.");
  }

  @Override
  public String toString() {
    return route().toString();
  }

  private ExchangeFn<FormData> formParser(final MediaType type, final Config config) {
    final ExchangeFn<FormData> parser;
    if (MediaType.form.name().equals(type.name())) {
      parser = (exchange) ->
          new FormEncodedDataDefinition()
              .setDefaultEncoding(charset.name())
              .create(exchange)
              .parseBlocking();
    } else if (MediaType.multipart.name().equals(type.name())) {
      parser = (exchange) -> new MultiPartParserDefinition()
          .setTempFileLocation(new File(config.getString("application.tmpdir")))
          .setDefaultEncoding(charset.name())
          .create(exchange)
          .parseBlocking();

    } else {
      parser = (exchange) -> new FormData(0);
    }
    return new MemoizingExchangeFn<FormData>(parser, new FormData(0));
  }

  private static Cookie cookie(final io.undertow.server.handlers.Cookie c) {
    Cookie.Definition cookie = new Cookie.Definition(c.getName(), c.getValue());
    Optional.ofNullable(c.getComment()).ifPresent(cookie::comment);
    Optional.ofNullable(c.getDomain()).ifPresent(cookie::domain);
    Optional.ofNullable(c.getPath()).ifPresent(cookie::path);
    Optional.ofNullable(c.getMaxAge()).ifPresent(cookie::maxAge);
    cookie.httpOnly(c.isHttpOnly());
    cookie.secure(c.isSecure());

    return cookie.toCookie();
  }

}
