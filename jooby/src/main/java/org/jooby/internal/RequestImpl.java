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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.BodyParser;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.internal.reqparam.BeanParamInjector;
import org.jooby.internal.reqparam.ParamResolver;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;
import org.jooby.util.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RequestImpl implements Request {

  private final Map<String, Mutant> params = new HashMap<>();

  private final List<MediaType> accept;

  private final Locale locale;

  private final MediaType type;

  private final Injector injector;

  private final NativeRequest req;

  private final Map<Object, Object> scope;

  private final Map<String, Object> locals;

  private final BodyConverterSelector selector;

  private Route route;

  private Session session;

  private Charset charset;

  public RequestImpl(final Injector injector,
      final NativeRequest req,
      final Route route,
      final Map<Object, Object> scope,
      final Map<String, Object> locals) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.req = requireNonNull(req, "An exchange is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.scope = requireNonNull(scope, "Scope is required.");
    this.locals = requireNonNull(locals, "Request locals are required.");
    this.selector = injector.getInstance(BodyConverterSelector.class);

    this.accept = findAccept(req);

    this.locale = findLocale(req, injector.getInstance(Locale.class));

    this.type = req.header("Content-Type")
        .map(MediaType::valueOf)
        .orElse(MediaType.all);

    this.charset = Optional.ofNullable(type.params().get("charset"))
        .map(Charset::forName)
        .orElse(injector.getInstance(Charset.class));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.get(name));
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
  public Map<String, Mutant> params() throws Exception {
    Map<String, Mutant> params = new LinkedHashMap<>();
    Set<String> names = new LinkedHashSet<>();
    for(Object name: route.vars().keySet()) {
      if (name instanceof String) {
        names.add((String) name);
      }
    }
    names.addAll(req.paramNames());
    for (String name : names) {
      params.put(name, param(name));
    }
    return params;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T params(final Class<T> beanType) throws Exception {
    // TODO: review me!
    return (T) BeanParamInjector.createAndInject(this, beanType);
  }

  @Override
  public Mutant param(final String name) throws Exception {
    Mutant param = this.params.get(name);
    if (param == null) {
      List<NativeUpload> files = req.files(name);
      if (files.size() > 0) {
        param = new MutantImpl(require(ParamResolver.class), req.files(name).stream()
            .map(upload -> new UploadImpl(injector, upload))
            .collect(Collectors.toList()));
      } else {
        List<String> values = new ArrayList<>();
        String pathvar = route.vars().get(name);
        if (pathvar != null) {
          values.add(pathvar);
        }
        values.addAll(req.params(name));
        param = new MutantImpl(require(ParamResolver.class), values);
      }
      this.params.put(name, param);
    }
    return param;
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "Header's name is missing.");
    return new MutantImpl(require(ParamResolver.class), req.headers(name));
  }

  @Override
  public Map<String, Mutant> headers() {
    Map<String, Mutant> headers = new LinkedHashMap<>();
    req.headerNames().forEach(name ->
        headers.put(name, header(name))
        );
    return headers;
  }

  @Override
  public Optional<Cookie> cookie(final String name) {
    return req.cookies().stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst();
  }

  @Override
  public List<Cookie> cookies() {
    return req.cookies();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    // TODO: review len
    if (length() > 0) {
      Optional<BodyParser> parser = selector.parser(type, ImmutableList.of(type()));
      if (parser.isPresent()) {
        return parser.get().parse(type, new BodyParserContext(charset(), () -> req.in()));
      }
      // TODO: review form and multipart post, we might not need them any more since we got .param
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
  public long length() {
    return req.header("Content-Length")
        .map(Long::parseLong)
        .orElse(-1L);
  }

  @Override
  public Locale locale() {
    return locale;
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
    return req.hostname();
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
  public String toString() {
    return route().toString();
  }

  private static List<MediaType> findAccept(final NativeRequest req) {
    List<MediaType> accept = req.header("Accept")
        .map(MediaType::parse)
        .orElse(MediaType.ALL);

    if (accept.size() > 1) {
      Collections.sort(accept);
    }
    return accept;
  }

  private static Locale findLocale(final NativeRequest req, final Locale def) {
    return req.header("Accept-Language")
        .map(l -> LocaleUtils.toLocale(l))
        .orElse(def);
  }

  void route(final Route route) {
    this.route = route;
  }
}
