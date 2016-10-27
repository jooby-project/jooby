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
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jooby.Cookie;
import org.jooby.Env;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.Upload;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.typesafe.config.Config;

import javaslang.control.Try;

public class RequestImpl implements Request {

  private final Map<String, Mutant> params = new HashMap<>();

  private final List<MediaType> accept;

  private final MediaType type;

  private final Injector injector;

  private final NativeRequest req;

  private final Map<Object, Object> scope;

  private final Map<String, Object> locals;

  private Route route;

  private Optional<Session> reqSession;

  private Charset charset;

  private List<File> files;

  private int port;

  private String contextPath;

  private Optional<String> lang;

  private List<Locale> locales;

  private long timestamp;

  public RequestImpl(final Injector injector, final NativeRequest req, final String contextPath,
      final int port, final Route route, final Charset charset, final List<Locale> locale,
      final Map<Object, Object> scope, final Map<String, Object> locals, final long timestamp) {
    this.injector = injector;
    this.req = req;
    this.route = route;
    this.scope = scope;
    this.locals = locals;

    this.contextPath = contextPath;

    Optional<String> accept = req.header("Accept");
    this.accept = accept.isPresent() ? MediaType.parse(accept.get()) : MediaType.ALL;

    this.lang = req.header("Accept-Language");
    this.locales = locale;

    this.port = port;

    Optional<String> type = req.header("Content-Type");
    this.type = type.isPresent() ? MediaType.valueOf(type.get()) : MediaType.all;

    String cs = this.type.params().get("charset");
    this.charset = cs != null ? Charset.forName(cs) : charset;

    this.files = new ArrayList<>();
    this.timestamp = timestamp;
  }

  @Override
  public String contextPath() {
    return contextPath;
  }

  @Override
  public Optional<String> queryString() {
    return req.queryString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> ifGet(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.get(name));
  }

  @Override
  public boolean matches(final String pattern) {
    RoutePattern p = new RoutePattern("*", pattern);
    return p.matcher(route.path()).matches();
  }

  @Override
  public Map<String, Object> attributes() {
    return Collections.unmodifiableMap(locals);
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
    return MediaType.matcher(accept()).first(types);
  }

  @Override
  public Mutant params(final String... xss) {
    return _params(xss(xss));
  }

  @Override
  public Mutant params() {
    return _params(null);
  }

  private Mutant _params(final Function<String, String> xss) {
    Map<String, Mutant> params = new HashMap<>();
    for (Object segment : route.vars().keySet()) {
      if (segment instanceof String) {
        String name = (String) segment;
        params.put(name, _param(name, xss));
      }
    }
    for (String name : paramNames()) {
      params.put(name, _param(name, xss));
    }
    return new MutantImpl(require(ParserExecutor.class), params);
  }

  @Override
  public Mutant param(final String name, final String... xss) {
    return _param(name, xss(xss));
  }

  @Override
  public Mutant param(final String name) {
    return _param(name, null);
  }

  private Mutant _param(final String name, final Function<String, String> xss) {
    Mutant param = this.params.get(name);
    if (param == null) {
      List<NativeUpload> files = Try.of(() -> req.files(name)).getOrElseThrow(
          ex -> new Err(Status.BAD_REQUEST, "Upload " + name + " resulted in error", ex));
      if (files.size() > 0) {
        List<Upload> uploads = files.stream()
            .map(upload -> new UploadImpl(injector, upload))
            .collect(Collectors.toList());
        param = new MutantImpl(require(ParserExecutor.class), type(),
            new UploadParamReferenceImpl(name, uploads));

        this.params.put(name, param);
      } else {
        StrParamReferenceImpl paramref = new StrParamReferenceImpl("parameter", name,
            params(name, xss));
        param = new MutantImpl(require(ParserExecutor.class), paramref);

        if (paramref.size() > 0) {
          this.params.put(name, param);
        }
      }
    }
    return param;
  }

  @Override
  public Mutant header(final String name) {
    return _header(name, null);
  }

  @Override
  public Mutant header(final String name, final String... xss) {
    return _header(name, xss(xss));
  }

  private Mutant _header(final String name, final Function<String, String> xss) {
    requireNonNull(name, "Name required.");
    List<String> headers = req.headers(name);
    if (xss != null) {
      headers = headers.stream()
          .map(xss::apply)
          .collect(Collectors.toList());
    }
    return new MutantImpl(require(ParserExecutor.class),
        new StrParamReferenceImpl("header", name, headers));
  }

  @Override
  public Map<String, Mutant> headers() {
    Map<String, Mutant> headers = new LinkedHashMap<>();
    req.headerNames().forEach(name -> headers.put(name, header(name)));
    return headers;
  }

  @Override
  public Mutant cookie(final String name) {
    List<String> values = req.cookies().stream().filter(c -> c.name().equalsIgnoreCase(name))
        .findFirst()
        .map(cookie -> ImmutableList.of(cookie.value().get()))
        .orElse(ImmutableList.of());

    return new MutantImpl(require(ParserExecutor.class),
        new StrParamReferenceImpl("cookie", name, values));
  }

  @Override
  public List<Cookie> cookies() {
    return req.cookies();
  }

  @Override
  public Mutant body() throws Exception {
    long length = length();
    if (length > 0) {
      MediaType type = type();
      Config conf = require(Config.class);

      File fbody = new File(conf.getString("application.tmpdir"),
          Integer.toHexString(System.identityHashCode(this)));
      files.add(fbody);
      int bufferSize = conf.getBytes("server.http.RequestBufferSize").intValue();
      Parser.BodyReference body = new BodyReferenceImpl(length, charset(), fbody, req.in(),
          bufferSize);
      return new MutantImpl(require(ParserExecutor.class), type, body);
    }
    return new MutantImpl(require(ParserExecutor.class), type, new EmptyBodyReference());
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
  public long length() {
    return req.header("Content-Length")
        .map(Long::parseLong)
        .orElse(-1L);
  }

  @Override
  public List<Locale> locales(
      final BiFunction<List<Locale.LanguageRange>, List<Locale>, List<Locale>> filter) {
    return lang.map(h -> filter.apply(LocaleUtils.range(h), locales))
        .orElseGet(() -> filter.apply(ImmutableList.of(), locales));
  }

  @Override
  public Locale locale(final BiFunction<List<LanguageRange>, List<Locale>, Locale> filter) {
    Supplier<Locale> def = () -> filter.apply(ImmutableList.of(), locales);
    // don't fail on bad Accept-Language header, just fallback to default locale.
    return lang.map(h -> Try.of(() -> filter.apply(LocaleUtils.range(h), locales)).getOrElse(def))
        .orElseGet(def);
  }

  @Override
  public String ip() {
    return req.ip();
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public String hostname() {
    return req.header("host").map(host -> host.split(":")[0]).orElse(ip());
  }

  @Override
  public int port() {
    return req.header("host").map(host -> {
      String[] parts = host.split(":");
      if (parts.length > 1) {
        return Integer.parseInt(parts[1]);
      }
      // fallback to default ports
      return secure() ? 443 : 80;
    }).orElse(port);
  }

  @Override
  public Session session() {
    return ifSession().orElseGet(() -> {
      SessionManager sm = require(SessionManager.class);
      Response rsp = require(Response.class);
      Session gsession = sm.create(this, rsp);
      return setSession(sm, rsp, gsession);
    });
  }

  @Override
  public Optional<Session> ifSession() {
    if (reqSession == null) {
      SessionManager sm = require(SessionManager.class);
      Response rsp = require(Response.class);
      Session gsession = sm.get(this, rsp);
      if (gsession == null) {
        reqSession = Optional.empty();
      } else {
        setSession(sm, rsp, gsession);
      }
    }
    return reqSession;
  }

  @Override
  public String protocol() {
    return req.protocol();
  }

  @Override
  public boolean secure() {
    return req.secure();
  }

  @Override
  public Request set(final String name, final Object value) {
    requireNonNull(name, "A local's name is required.");
    requireNonNull(value, "A local's value is required.");
    locals.put(name, value);
    return this;
  }

  @Override
  public Request set(final Key<?> key, final Object value) {
    requireNonNull(key, "A local's jey is required.");
    requireNonNull(value, "A local's value is required.");
    scope.put(key, value);
    return this;
  }

  @Override
  public Request push(final String path, final Map<String, Object> headers) {
    if (protocol().equalsIgnoreCase("HTTP/2.0")) {
      require(Response.class).after((req, rsp, value) -> {
        this.req.push("GET", contextPath + path, headers);
        return value;
      });
      return this;
    } else {
      throw new UnsupportedOperationException("Push promise not available");
    }
  }

  @Override
  public String toString() {
    return route().toString();
  }

  private List<String> paramNames() {
    try {
      return req.paramNames();
    } catch (Exception ex) {
      throw new Err(Status.BAD_REQUEST, "Unable to get parameter names", ex);
    }
  }

  private Function<String, String> xss(final String... xss) {
    return require(Env.class).xss(xss);
  }

  private List<String> params(final String name, final Function<String, String> xss) {
    try {
      List<String> values = new ArrayList<>();
      String pathvar = route.vars().get(name);
      if (pathvar != null) {
        values.add(pathvar);
      }
      values.addAll(req.params(name));
      if (xss == null) {
        return values;
      }
      for (int i = 0; i < values.size(); i++) {
        values.set(i, xss.apply(values.get(i)));
      }
      return values;
    } catch (Throwable ex) {
      throw new Err(Status.BAD_REQUEST, "Parameter '" + name + "' resulted in error", ex);
    }
  }

  void route(final Route route) {
    this.route = route;
  }

  public void done() {
    if (reqSession != null) {
      reqSession.ifPresent(session -> require(SessionManager.class).requestDone(session));
    }
    if (files.size() > 0) {
      for (File file : files) {
        file.delete();
      }
    }
  }

  @Override
  public long timestamp() {
    return timestamp;
  }

  private Session setSession(final SessionManager sm, final Response rsp, final Session gsession) {
    Session rsession = new RequestScopedSession(sm, rsp, gsession, this::destroySession);
    reqSession = Optional.of(rsession);
    return rsession;
  }

  private void destroySession() {
    this.reqSession = Optional.empty();
  }

}
