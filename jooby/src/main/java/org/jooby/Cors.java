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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.util.Collectors;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

/**
 * <h1>Cross-origin resource sharing</h1>
 * <p>
 * Cross-origin resource sharing (CORS) is a mechanism that allows restricted resources (e.g. fonts,
 * JavaScript, etc.) on a web page to be requested from another domain outside the domain from which
 * the resource originated.
 * </p>
 *
 * <p>
 * This class represent the available options for configure CORS in Jooby.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   cors();
 * }
 * </pre>
 *
 * <p>
 * Previous example, adds a cors filter using the default cors options. See {@link Jooby#cors()}.
 * </p>
 *
 * @author edgar
 * @since 0.8.0
 */
public class Cors {

  private static class Matcher<T> implements Predicate<T> {

    private List<String> values;

    private Predicate<T> predicate;

    private boolean wild;

    public Matcher(final List<String> values, final Predicate<T> predicate) {
      this.values = ImmutableList.copyOf(values);
      this.predicate = predicate;
      this.wild = values.contains("*");
    }

    @Override
    public boolean test(final T value) {
      return predicate.test(value);
    }

  }

  private boolean enabled;

  private Matcher<String> origin;

  private boolean credentials;

  private Matcher<String> requestMehods;

  private Matcher<List<String>> requestHeaders;

  private int maxAge;

  private List<String> exposedHeaders;

  /**
   * Creates {@link Cors} options from {@link Config}:
   *
   * <pre>
   *  origin: "*"
   *  credentials: true
   *  allowedMethods: [GET, POST]
   *  allowedHeaders: [X-Requested-With, Content-Type, Accept, Origin]
   *  exposedHeaders: []
   * </pre>
   *
   * @param config Config to use.
   */
  @Inject
  public Cors(@Named("cors") final Config config) {
    requireNonNull(config, "Config is required.");
    this.enabled = config.hasPath("enabled") ? config.getBoolean("enabled") : true;
    withOrigin(list(config.getAnyRef("origin")));
    this.credentials = config.getBoolean("credentials");
    withMethods(list(config.getAnyRef("allowedMethods")));
    withHeaders(list(config.getAnyRef("allowedHeaders")));
    withMaxAge((int) config.getDuration("maxAge", TimeUnit.SECONDS));
    withExposedHeaders(config.hasPath("exposedHeaders")
        ? list(config.getAnyRef("exposedHeaders"))
        : Collections.emptyList());
  }

  /**
   * Creates default {@link Cors}. Default options are:
   *
   * <pre>
   *  origin: "*"
   *  credentials: true
   *  allowedMethods: [GET, POST]
   *  allowedHeaders: [X-Requested-With, Content-Type, Accept, Origin]
   *  exposedHeaders: []
   * </pre>
   */
  public Cors() {
    this.enabled = true;
    withOrigin("*");
    credentials = true;
    withMethods("GET", "POST");
    withHeaders("X-Requested-With", "Content-Type", "Accept", "Origin");
    withMaxAge(1800);
    withExposedHeaders();
  }

  /**
   * Set {@link #credentials()} to false.
   *
   * @return This cors.
   */
  public Cors withoutCreds() {
    this.credentials = false;
    return this;
  }

  /**
   * @return True, if cors is enabled. Controlled by: <code>cors.enabled</code> property. Default
   *         is: <code>true</code>.
   */
  public boolean enabled() {
    return enabled;
  }

  /**
   * Disabled cors (enabled = false).
   *
   * @return This cors.
   */
  public Cors disabled() {
    enabled = false;
    return this;
  }

  /**
   * If true, set the <code>Access-Control-Allow-Credentials</code> header. Controlled by:
   * <code>cors.credentials</code> property. Default is: <code>true</code>
   *
   * @return If the <code>Access-Control-Allow-Credentials</code> header must be set.
   */
  public boolean credentials() {
    return this.credentials;
  }

  /**
   * @return True if any origin is accepted.
   */
  public boolean anyOrigin() {
    return origin.wild;
  }

  /**
   * An origin must be a "*" (any origin), a domain name (like, http://foo.com) and/or a regex
   * (like, http://*.domain.com).
   *
   * @return List of valid origins: Default is: <code>*</code>
   */
  public List<String> origin() {
    return origin.values;
  }

  /**
   * Test if the given origin is allowed or not.
   *
   * @param origin The origin to test.
   * @return
   */
  public boolean allowOrigin(final String origin) {
    return this.origin.test(origin);
  }

  /**
   * Set the allowed origins. An origin must be a "*" (any origin), a domain name (like,
   * http://foo.com) and/or a regex (like, http://*.domain.com).
   *
   * @param origin One ore more origin.
   * @return This cors.
   */
  public Cors withOrigin(final String... origin) {
    return withOrigin(Arrays.asList(origin));
  }

  /**
   * Set the allowed origins. An origin must be a "*" (any origin), a domain name (like,
   * http://foo.com) and/or a regex (like, http://*.domain.com).
   *
   * @param origin One ore more origin.
   * @return This cors.
   */
  public Cors withOrigin(final List<String> origin) {
    this.origin = firstMatch(requireNonNull(origin, "Origins are required."));
    return this;
  }

  /**
   * True if the method is allowed.
   *
   * @param method Method to test.
   * @return True if the method is allowed.
   */
  public boolean allowMethod(final String method) {
    return this.requestMehods.test(method);
  }

  /**
   * @return List of allowed methods.
   */
  public List<String> allowedMethods() {
    return requestMehods.values;
  }

  /**
   * Set one or more allowed methods.
   *
   * @param methods One or more method.
   * @return This cors.
   */
  public Cors withMethods(final String... methods) {
    return withMethods(Arrays.asList(methods));
  }

  /**
   * Set one or more allowed methods.
   *
   * @param methods One or more method.
   * @return This cors.
   */
  public Cors withMethods(final List<String> methods) {
    this.requestMehods = firstMatch(methods);
    return this;
  }

  /**
   * @return True if any header is allowed: <code>*</code>.
   */
  public boolean anyHeader() {
    return requestHeaders.wild;
  }

  /**
   * @param header A header to test.
   * @return True if a header is allowed.
   */
  public boolean allowHeader(final String header) {
    return allowHeaders(ImmutableList.of(header));
  }

  /**
   * True if all the headers are allowed.
   *
   * @param headers Headers to test.
   * @return True if all the headers are allowed.
   */
  public boolean allowHeaders(final String... headers) {
    return allowHeaders(Arrays.asList(headers));
  }

  /**
   * True if all the headers are allowed.
   *
   * @param headers Headers to test.
   * @return True if all the headers are allowed.
   */
  public boolean allowHeaders(final List<String> headers) {
    return this.requestHeaders.test(headers);
  }

  /**
   * @return List of allowed headers. Default are: <code>X-Requested-With</code>,
   *         <code>Content-Type</code>, <code>Accept</code> and <code>Origin</code>.
   */
  public List<String> allowedHeaders() {
    return requestHeaders.values;
  }

  /**
   * Set one or more allowed headers. Possible values are a header name or <code>*</code> if any
   * header is allowed.
   *
   * @param headers Headers to set.
   * @return This cors.
   */
  public Cors withHeaders(final String... headers) {
    return withHeaders(Arrays.asList(headers));
  }

  /**
   * Set one or more allowed headers. Possible values are a header name or <code>*</code> if any
   * header is allowed.
   *
   * @param headers Headers to set.
   * @return This cors.
   */
  public Cors withHeaders(final List<String> headers) {
    this.requestHeaders = allMatch(headers);
    return this;
  }

  /**
   * @return List of exposed headers.
   */
  public List<String> exposedHeaders() {
    return exposedHeaders;
  }

  /**
   * Set the list of exposed headers.
   *
   * @param exposedHeaders Headers to expose.
   * @return This cors.
   */
  public Cors withExposedHeaders(final String... exposedHeaders) {
    return withExposedHeaders(Arrays.asList(exposedHeaders));
  }

  /**
   * Set the list of exposed headers.
   *
   * @param exposedHeaders Headers to expose.
   * @return This cors.
   */
  public Cors withExposedHeaders(final List<String> exposedHeaders) {
    this.exposedHeaders = requireNonNull(exposedHeaders, "Exposed headers are required.");
    return this;
  }

  /**
   * @return Preflight max age. How many seconds a client can cache a preflight request.
   */
  public int maxAge() {
    return maxAge;
  }

  /**
   * Set the preflight max age header. That's how many seconds a client can cache a preflight
   * request.
   *
   * @param preflightMaxAge Number of seconds or <code>-1</code> to turn this off.
   * @return This cors.
   */
  public Cors withMaxAge(final int preflightMaxAge) {
    this.maxAge = preflightMaxAge;
    return this;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private List<String> list(final Object value) {
    return value instanceof List ? (List) value : ImmutableList.of(value.toString());
  }

  private static Matcher<List<String>> allMatch(final List<String> values) {
    Predicate<String> predicate = firstMatch(values);
    Predicate<List<String>> allmatch = it -> it.stream().allMatch(predicate);
    return new Matcher<List<String>>(values, allmatch);
  }

  private static Matcher<String> firstMatch(final List<String> values) {
    List<Pattern> patterns = values.stream()
        .map(Cors::rewrite)
        .collect(Collectors.toList());
    Predicate<String> predicate = it -> patterns.stream()
        .filter(pattern -> pattern.matcher(it).matches())
        .findFirst()
        .isPresent();

    return new Matcher<String>(values, predicate);
  }

  private static Pattern rewrite(final String origin) {
    return Pattern.compile(origin.replace(".", "\\.").replace("*", ".*"), Pattern.CASE_INSENSITIVE);
  }

}
