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
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;

import org.jooby.scope.RequestScoped;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Give you access at the current HTTP request in order to read parameters, headers and body.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Request extends Registry {

  /**
   * Forwarding request.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Forwarding implements Request {

    /** Target request. */
    private Request req;

    /**
     * Creates a new {@link Forwarding} request.
     *
     * @param request A target request.
     */
    public Forwarding(final Request request) {
      this.req = requireNonNull(request, "A HTTP request is required.");
    }

    @Override
    public String path() {
      return req.path();
    }

    @Override
    public boolean matches(final String pattern) {
      return req.matches(pattern);
    }

    @Override
    public String contextPath() {
      return req.contextPath();
    }

    @Override
    public String method() {
      return req.method();
    }

    @Override
    public MediaType type() {
      return req.type();
    }

    @Override
    public List<MediaType> accept() {
      return req.accept();
    }

    @Override
    public Optional<MediaType> accepts(final List<MediaType> types) {
      return req.accepts(types);
    }

    @Override
    public Optional<MediaType> accepts(final MediaType... types) {
      return req.accepts(types);
    }

    @Override
    public Optional<MediaType> accepts(final String... types) {
      return req.accepts(types);
    }

    @Override
    public boolean is(final List<MediaType> types) {
      return req.is(types);
    }

    @Override
    public boolean is(final MediaType... types) {
      return req.is(types);
    }

    @Override
    public boolean is(final String... types) {
      return req.is(types);
    }

    @Override
    public boolean isSet(final String name) {
      return req.isSet(name);
    };

    @Override
    public Mutant params() {
      return req.params();
    }

    @Override
    public <T> T params(final Class<T> type) {
      return req.params(type);
    }

    @Override
    public Mutant param(final String name) {
      return req.param(name);
    }

    @Override
    public Upload file(final String name) {
      return req.file(name);
    }

    @Override
    public List<Upload> files(final String name) {
      return req.files(name);
    }

    @Override
    public Mutant header(final String name) {
      return req.header(name);
    }

    @Override
    public Map<String, Mutant> headers() {
      return req.headers();
    }

    @Override
    public Mutant cookie(final String name) {
      return req.cookie(name);
    }

    @Override
    public List<Cookie> cookies() {
      return req.cookies();
    }

    @Override
    public Mutant body() throws Exception {
      return req.body();
    }

    @Override
    public <T> T body(final Class<T> type) throws Exception {
      return req.body(type);
    }

    @Override
    public <T> T require(final Class<T> type) {
      return req.require(type);
    }

    @Override
    public <T> T require(final TypeLiteral<T> type) {
      return req.require(type);
    }

    @Override
    public <T> T require(final Key<T> key) {
      return req.require(key);
    }

    @Override
    public Charset charset() {
      return req.charset();
    }

    @Override
    public long length() {
      return req.length();
    }

    @Override
    public Locale locale() {
      return req.locale();
    }

    @Override
    public Locale locale(final BiFunction<List<LanguageRange>, List<Locale>, Locale> filter) {
      return req.locale(filter);
    }

    @Override
    public List<Locale> locales(
        final BiFunction<List<LanguageRange>, List<Locale>, List<Locale>> filter) {
      return req.locales(filter);
    }

    @Override
    public List<Locale> locales() {
      return req.locales();
    }

    @Override
    public String ip() {
      return req.ip();
    }

    @Override
    public int port() {
      return req.port();
    }

    @Override
    public Route route() {
      return req.route();
    }

    @Override
    public Session session() {
      return req.session();
    }

    @Override
    public Optional<Session> ifSession() {
      return req.ifSession();
    }

    @Override
    public String hostname() {
      return req.hostname();
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
    public boolean xhr() {
      return req.xhr();
    }

    @Override
    public Map<String, Object> attributes() {
      return req.attributes();
    }

    @Override
    public <T> Optional<T> ifGet(final String name) {
      return req.ifGet(name);
    }

    @Override
    public <T> T get(final String name) {
      return req.get(name);
    }

    @Override
    public <T> T get(final String name, final T def) {
      return req.get(name, def);
    }

    @Override
    public Request set(final String name, final Object value) {
      req.set(name, value);
      return this;
    }

    @Override
    public Request set(final Key<?> key, final Object value) {
      req.set(key, value);
      return this;
    }

    @Override
    public Request set(final Class<?> type, final Object value) {
      req.set(type, value);
      return this;
    }

    @Override
    public Request set(final TypeLiteral<?> type, final Object value) {
      req.set(type, value);
      return this;
    }

    @Override
    public <T> Optional<T> unset(final String name) {
      return req.unset(name);
    }

    @Override
    public Map<String, String> flash() throws NoSuchElementException {
      return req.flash();
    }

    @Override
    public String flash(final String name) throws NoSuchElementException {
      return req.flash(name);
    }

    @Override
    public Request flash(final String name, final Object value) {
      req.flash(name, value);
      return this;
    }

    @Override
    public Optional<String> ifFlash(final String name) {
      return req.ifFlash(name);
    }

    @Override
    public void push(final String path) {
      req.push(path);
    }

    @Override
    public void push(final String path, final Map<String, String> headers) {
      req.push(path, headers);
    }

    @Override
    public void push(final String method, final String path, final Map<String, String> headers) {
      req.push(method, path, headers);
    }

    @Override
    public String toString() {
      return req.toString();
    }

    /**
     * Unwrap a request in order to find out the target instance.
     *
     * @param req A request.
     * @return A target instance (not a {@link Forwarding}).
     */
    public static Request unwrap(final Request req) {
      requireNonNull(req, "A request is required.");
      Request root = req;
      while (root instanceof Forwarding) {
        root = ((Forwarding) root).req;
      }
      return root;
    }
  }

  /**
   * Given:
   *
   * <pre>
   *  http://domain.com/some/path.html {@literal ->} /some/path.html
   *  http://domain.com/a.html         {@literal ->} /a.html
   * </pre>
   *
   * @return The request URL pathname.
   */
  default String path() {
    return route().path();
  }

  /**
   * Application path (a.k.a context path). It is the value defined by:
   * <code>application.path</code>. Default is: <code>/</code>
   *
   * @return Application context path..
   */
  String contextPath();

  /**
   * @return HTTP method.
   */
  default String method() {
    return route().method();
  }

  /**
   * @return The <code>Content-Type</code> header. Default is: {@literal*}/{@literal*}.
   */
  MediaType type();

  /**
   * @return The value of the <code>Accept header</code>. Default is: {@literal*}/{@literal*}.
   */
  List<MediaType> accept();

  /**
   * Check if the given types are acceptable, returning the best match when true, or else
   * Optional.empty.
   *
   * <pre>
   * // Accept: text/html
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   *
   * // Accept: text/*, application/json
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("application/json" "text/plain");
   * // {@literal =>} "application/json"
   * req.accepts("application/json");
   * // {@literal =>} "application/json"
   *
   * // Accept: text/*, application/json
   * req.accepts("image/png");
   * // {@literal =>} Optional.empty
   *
   * // Accept: text/*;q=.5, application/json
   * req.accepts("text/html", "application/json");
   * // {@literal =>} "application/json"
   * </pre>
   *
   * @param types Types to test.
   * @return The best acceptable type.
   */
  default Optional<MediaType> accepts(final String... types) {
    return accepts(MediaType.valueOf(types));
  }

  /**
   * Test if the given request path matches the pattern.
   *
   * @param pattern A pattern to test for.
   * @return True, if the request path matches the pattern.
   */
  boolean matches(String pattern);

  /**
   * True, if request accept any of the given types.
   *
   * @param types Types to test
   * @return True if any of the given type is accepted.
   */
  default boolean is(final String... types) {
    return accepts(types).isPresent();
  }

  /**
   * True, if request accept any of the given types.
   *
   * @param types Types to test
   * @return True if any of the given type is accepted.
   */
  default boolean is(final MediaType... types) {
    return accepts(types).isPresent();
  }

  /**
   * True, if request accept any of the given types.
   *
   * @param types Types to test
   * @return True if any of the given type is accepted.
   */
  default boolean is(final List<MediaType> types) {
    return accepts(types).isPresent();
  }

  /**
   * Check if the given types are acceptable, returning the best match when true, or else
   * Optional.empty.
   *
   * <pre>
   * // Accept: text/html
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   *
   * // Accept: text/*, application/json
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("application/json" "text/plain");
   * // {@literal =>} "application/json"
   * req.accepts("application/json");
   * // {@literal =>} "application/json"
   *
   * // Accept: text/*, application/json
   * req.accepts("image/png");
   * // {@literal =>} Optional.empty
   *
   * // Accept: text/*;q=.5, application/json
   * req.accepts("text/html", "application/json");
   * // {@literal =>} "application/json"
   * </pre>
   *
   * @param types Types to test.
   * @return The best acceptable type.
   */
  default Optional<MediaType> accepts(final MediaType... types) {
    return accepts(ImmutableList.copyOf(types));
  }

  /**
   * Check if the given types are acceptable, returning the best match when true, or else
   * Optional.empty.
   *
   * <pre>
   * // Accept: text/html
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   *
   * // Accept: text/*, application/json
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("text/html");
   * // {@literal =>} "text/html"
   * req.accepts("application/json" "text/plain");
   * // {@literal =>} "application/json"
   * req.accepts("application/json");
   * // {@literal =>} "application/json"
   *
   * // Accept: text/*, application/json
   * req.accepts("image/png");
   * // {@literal =>} Optional.empty
   *
   * // Accept: text/*;q=.5, application/json
   * req.accepts("text/html", "application/json");
   * // {@literal =>} "application/json"
   * </pre>
   *
   * @param types Types to test for.
   * @return The best acceptable type.
   */
  Optional<MediaType> accepts(List<MediaType> types);

  /**
   * Get all the available parameters. A HTTP parameter can be provided in any of
   * these forms:
   *
   * <ul>
   * <li>Path parameter, like: <code>/path/:name</code> or <code>/path/{name}</code></li>
   * <li>Query parameter, like: <code>?name=jooby</code></li>
   * <li>Body parameter when <code>Content-Type</code> is
   * <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code></li>
   * </ul>
   *
   * @return All the parameters.
   */
  Mutant params();

  /**
   * Short version of <code>params().to(type)</code>.
   *
   * @param type Object type.
   * @param <T> Value type.
   * @return Instance of object.
   */
  default <T> T params(final Class<T> type) {
    return params().to(type);
  }

  /**
   * Get a HTTP request parameter under the given name. A HTTP parameter can be provided in any of
   * these forms:
   * <ul>
   * <li>Path parameter, like: <code>/path/:name</code> or <code>/path/{name}</code></li>
   * <li>Query parameter, like: <code>?name=jooby</code></li>
   * <li>Body parameter when <code>Content-Type</code> is
   * <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code></li>
   * </ul>
   *
   * The order of precedence is: <code>path</code>, <code>query</code> and <code>body</code>. For
   * example a pattern like: <code>GET /path/:name</code> for <code>/path/jooby?name=rocks</code>
   * produces:
   *
   * <pre>
   *  assertEquals("jooby", req.param(name).value());
   *
   *  assertEquals("jooby", req.param(name).toList().get(0));
   *  assertEquals("rocks", req.param(name).toList().get(1));
   * </pre>
   *
   * Uploads can be retrieved too when <code>Content-Type</code> is <code>multipart/form-data</code>
   * see {@link Upload} for more information.
   *
   * @param name A parameter's name.
   * @return A HTTP request parameter.
   */
  Mutant param(String name);

  /**
   * Get a file {@link Upload} with the given name. The request must be a POST with
   * <code>multipart/form-data</code> content-type.
   *
   * @param name File's name.
   * @return An {@link Upload}.
   */
  default Upload file(final String name) {
    return param(name).toUpload();
  }

  /**
   * Get a list of file {@link Upload} with the given name. The request must be a POST with
   * <code>multipart/form-data</code> content-type.
   *
   * @param name File's name.
   * @return A list of {@link Upload}.
   */
  default List<Upload> files(final String name) {
    return param(name).toList(Upload.class);
  }

  /**
   * Get a HTTP header.
   *
   * @param name A header's name.
   * @return A HTTP request header.
   */
  Mutant header(String name);

  /**
   * @return All the headers.
   */
  Map<String, Mutant> headers();

  /**
   * Get a cookie with the given name (if present).
   *
   * @param name Cookie's name.
   * @return A cookie or an empty optional.
   */
  Mutant cookie(String name);

  /**
   * @return All the cookies.
   */
  List<Cookie> cookies();

  /**
   * HTTP body.
   *
   * @return The HTTP body.
   * @throws Exception If body can't be converted or there is no HTTP body.
   */
  Mutant body() throws Exception;

  /**
   * Short version of <code>body().to(type)</code>.
   *
   * @param type Object type.
   * @param <T> Value type.
   * @return Instance of object.
   * @throws Exception If body can't be converted or there is no HTTP body.
   */
  default <T> T body(final Class<T> type) throws Exception {
    return body().to(type);
  }

  /**
   * The charset defined in the request body. If the request doesn't specify a character
   * encoding, this method return the global charset: <code>application.charset</code>.
   *
   * @return A current charset.
   */
  Charset charset();

  /**
   * Get a list of locale that best matches the current request as per {@link Locale#filter}.
   *
   * @return A list of matching locales or empty list.
   */
  default List<Locale> locales() {
    return locales(Locale::filter);
  }

  /**
   * Get a list of locale that best matches the current request.
   *
   * The first filter argument is the value of <code>Accept-Language</code> as
   * {@link Locale.LanguageRange} and filter while the second argument is a list of supported
   * locales defined by the <code>application.lang</code> property.
   *
   * The next example returns a list of matching {@code Locale} instances using the filtering
   * mechanism defined in RFC 4647:
   *
   * <pre>{@code
   * req.locales(Locale::filter)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A list of matching locales.
   */
  List<Locale> locales(BiFunction<List<Locale.LanguageRange>, List<Locale>, List<Locale>> filter);

  /**
   * Get a locale that best matches the current request.
   *
   * The first filter argument is the value of <code>Accept-Language</code> as
   * {@link Locale.LanguageRange} and filter while the second argument is a list of supported
   * locales defined by the <code>application.lang</code> property.
   *
   * The next example returns a {@code Locale} instance for the best-matching language
   * tag using the lookup mechanism defined in RFC 4647.
   *
   * <pre>{@code
   * req.locale(Locale::lookup)
   * }</pre>
   *
   * @param filter A locale filter.
   * @return A matching locale.
   */
  Locale locale(BiFunction<List<Locale.LanguageRange>, List<Locale>, Locale> filter);

  /**
   * Get a locale that best matches the current request or the default locale as specified
   * in <code>application.lang</code>.
   *
   * @return A matching locale.
   */
  default Locale locale() {
    return locale((ranges, locales) -> Locale.filter(ranges, locales).stream()
        .findFirst()
        .orElse(locales.get(0)));
  }

  /**
   * @return The length, in bytes, of the request body and made available by the input stream, or
   *         <code>-1</code> if the length is not known.
   */
  long length();

  /**
   * @return The IP address of the client or last proxy that sent the request.
   */
  String ip();

  /**
   * @return Server port, from <code>host</code> header or the server port where the client
   *         connection was accepted on.
   */
  int port();

  /**
   * @return The currently matched {@link Route}.
   */
  Route route();

  /**
   * The fully qualified name of the resource being requested, as obtained from the Host HTTP
   * header.
   *
   * @return The fully qualified name of the server.
   */
  String hostname();

  /**
   * @return The current session associated with this request or if the request does not have a
   *         session, creates one.
   */
  Session session();

  /**
   * @return The current session associated with this request if there is one.
   */
  Optional<Session> ifSession();

  /**
   * @return True if the <code>X-Requested-With</code> header is set to <code>XMLHttpRequest</code>.
   */
  default boolean xhr() {
    return header("X-Requested-With")
        .toOptional(String.class)
        .map("XMLHttpRequest"::equalsIgnoreCase)
        .orElse(Boolean.FALSE);
  }

  /**
   * @return The name and version of the protocol the request uses in the form
   *         <i>protocol/majorVersion.minorVersion</i>, for example, HTTP/1.1
   */
  String protocol();

  /**
   * @return True if this request was made using a secure channel, such as HTTPS.
   */
  boolean secure();

  /**
   * Set local attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's local. NOT null.
   * @return This request.
   */
  Request set(String name, Object value);

  /**
   * Give you access to flash scope. Usage:
   *
   * <pre>{@code
   * {
   *   use(new FlashScope());
   *
   *   get("/", req -> {
   *     Map<String, String> flash = req.flash();
   *     return flash;
   *   });
   * }
   * }</pre>
   *
   * As you can see in the example above, the {@link FlashScope} needs to be install it by calling
   * {@link Jooby#use(org.jooby.Jooby.Module)} otherwise a call to this method ends in
   * {@link Err BAD_REQUEST}.
   *
   * @return A mutable map with attributes from {@link FlashScope}.
   * @throws Err Bad request error if the {@link FlashScope} was not installed it.
   */
  default Map<String, String> flash() throws Err {
    Optional<Map<String, String>> flash = ifGet(FlashScope.NAME);
    return flash.orElseThrow(() -> new Err(Status.BAD_REQUEST,
        "Flash scope isn't available. Install via: use(new FlashScope());"));
  }

  /**
   * Set a flash attribute. Flash scope attributes are accessible from template engines, by
   * prefixing attributes with <code>flash.</code>. For example a call to
   * <code>flash("success", "OK")</code> is accessible from template engines using
   * <code>flash.success</code>
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This request.
   */
  default Request flash(final String name, final Object value) {
    requireNonNull(name, "Attribute's name is required.");
    Map<String, String> flash = flash();
    if (value == null) {
      flash.remove(name);
    } else {
      flash.put(name, value.toString());
    }
    return this;
  }

  /**
   * Get an optional for the given flash attribute's name.
   *
   * @param name Attribute's name.
   * @return Optional flash attribute.
   */
  default Optional<String> ifFlash(final String name) {
    return Optional.ofNullable(flash().get(name));
  }

  /**
   * Get a flash attribute value or throws {@link Err BAD_REQUEST error} if missing.
   *
   * @param name Attribute's name.
   * @return Flash attribute.
   * @throws Err Bad request error if flash attribute is missing.
   */
  default String flash(final String name) throws Err {
    return ifFlash(name)
        .orElseThrow(() -> new Err(Status.BAD_REQUEST,
            "Required flash attribute: '" + name + "' is not present"));
  }

  /**
   * @param name Attribute's name.
   * @return True if the local attribute is set.
   */
  default boolean isSet(final String name) {
    return ifGet(name).isPresent();
  }

  /**
   * Get a request local attribute.
   *
   * @param name Attribute's name.
   * @param <T> Target type.
   * @return A local attribute.
   */
  <T> Optional<T> ifGet(String name);

  /**
   * Get a request local attribute.
   *
   * @param name Attribute's name.
   * @param def A default value.
   * @param <T> Target type.
   * @return A local attribute.
   */
  default <T> T get(final String name, final T def) {
    Optional<T> opt = ifGet(name);
    return opt.orElse(def);
  }

  /**
   * Get a request local attribute.
   *
   * @param name Attribute's name.
   * @param <T> Target type.
   * @return A local attribute.
   * @throws Err with {@link Status#BAD_REQUEST}.
   */
  default <T> T get(final String name) {
    Optional<T> opt = ifGet(name);
    return opt.orElseThrow(
        () -> new Err(Status.BAD_REQUEST, "Required local attribute: " + name + " is not present"));
  }

  /**
   * Remove a request local attribute.
   *
   * @param name Attribute's name.
   * @param <T> Target type.
   * @return A local attribute.
   */
  <T> Optional<T> unset(String name);

  /**
   * A read only version of the current locals.
   *
   * @return Attributes locals.
   */
  Map<String, Object> attributes();

  /**
   * Seed a {@link RequestScoped} object.
   *
   * @param type Object type.
   * @param value Actual object to bind.
   * @return Current request.
   */
  default Request set(final Class<?> type, final Object value) {
    return set(TypeLiteral.get(type), value);
  }

  /**
   * Seed a {@link RequestScoped} object.
   *
   * @param type Seed type.
   * @param value Actual object to bind.
   * @return Current request.
   */
  default Request set(final TypeLiteral<?> type, final Object value) {
    return set(Key.get(type), value);
  }

  /**
   * Seed a {@link RequestScoped} object.
   *
   * @param key Seed key.
   * @param value Actual object to bind.
   * @return Current request.
   */
  Request set(Key<?> key, Object value);

  default void push(final String path) {
    push(path, ImmutableMap.of());
  }

  default void push(final String path, final Map<String, String> headers) {
    push("GET", path, headers);
  }

  void push(final String method, final String path, final Map<String, String> headers);
}
