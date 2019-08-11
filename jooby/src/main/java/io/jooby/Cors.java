/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
 *   decorator(new CorsHandler());
 * }
 * </pre>
 *
 * <p>
 * Previous example, adds a cors filter using the default cors options.
 * </p>
 *
 * @author edgar
 * @since 2.0.4
 */
public class Cors {

  private static class Matcher<T> implements Predicate<T> {

    private List<String> values;

    private Predicate<T> predicate;

    private boolean wild;

    public Matcher(final List<String> values, final Predicate<T> predicate) {
      this.values = values;
      this.predicate = predicate;
      this.wild = values.contains("*");
    }

    @Override
    public boolean test(final T value) {
      return predicate.test(value);
    }

    @Override public String toString() {
      return values.toString();
    }
  }

  private Matcher<String> origin;

  private boolean credentials;

  private Matcher<String> methods;

  private Matcher<List<String>> headers;

  private Duration maxAge;

  private List<String> exposedHeaders = Collections.emptyList();

  /**
   * Creates default {@link Cors}. Default options are:
   *
   * <pre>
   *  origin: "*"
   *  credentials: true
   *  allowedMethods: [GET, POST]
   *  allowedHeaders: [X-Requested-With, Content-Type, Accept, Origin]
   *  maxAge: 30m
   *  exposedHeaders: []
   * </pre>
   */
  public Cors() {
    setOrigin("*");
    setUseCredentials(true);
    setMethods("GET", "POST");
    setHeaders("X-Requested-With", "Content-Type", "Accept", "Origin");
    setMaxAge(Duration.ofMinutes(30));
  }

  /**
   * If true, set the <code>Access-Control-Allow-Credentials</code> header.
   *
   * @return If the <code>Access-Control-Allow-Credentials</code> header must be set.
   */
  public boolean getUseCredentials() {
    return this.credentials;
  }

  public Cors setUseCredentials(boolean credentials) {
    this.credentials = credentials;
    return this;
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
  public List<String> getOrigin() {
    return origin.values;
  }

  /**
   * Test if the given origin is allowed or not.
   *
   * @param origin The origin to test.
   * @return True if the origin is allowed.
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
  public Cors setOrigin(final String... origin) {
    return setOrigin(Arrays.asList(origin));
  }

  /**
   * Set the allowed origins. An origin must be a "*" (any origin), a domain name (like,
   * http://foo.com) and/or a regex (like, http://*.domain.com).
   *
   * @param origin One ore more origin.
   * @return This cors.
   */
  public Cors setOrigin(final List<String> origin) {
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
    return this.methods.test(method);
  }

  /**
   * @return List of allowed methods.
   */
  public List<String> getMethods() {
    return methods.values;
  }

  /**
   * Set one or more allowed methods.
   *
   * @param methods One or more method.
   * @return This cors.
   */
  public Cors setMethods(final String... methods) {
    return setMethods(Arrays.asList(methods));
  }

  /**
   * Set one or more allowed methods.
   *
   * @param methods One or more method.
   * @return This cors.
   */
  public Cors setMethods(final List<String> methods) {
    this.methods = firstMatch(methods);
    return this;
  }

  /**
   * @return True if any header is allowed: <code>*</code>.
   */
  public boolean anyHeader() {
    return headers.wild;
  }

  /**
   * True if all the headers are allowed.
   *
   * @param headers Headers to test.
   * @return True if all the headers are allowed.
   */
  public boolean allowHeader(final String... headers) {
    return allowHeaders(Arrays.asList(headers));
  }

  /**
   * True if all the headers are allowed.
   *
   * @param headers Headers to test.
   * @return True if all the headers are allowed.
   */
  public boolean allowHeaders(final List<String> headers) {
    return this.headers.test(headers);
  }

  /**
   * @return List of allowed headers. Default are: <code>X-Requested-With</code>,
   *         <code>Content-Type</code>, <code>Accept</code> and <code>Origin</code>.
   */
  public List<String> getHeaders() {
    return headers.values;
  }

  /**
   * Set one or more allowed headers. Possible values are a header name or <code>*</code> if any
   * header is allowed.
   *
   * @param headers Headers to set.
   * @return This cors.
   */
  public Cors setHeaders(final String... headers) {
    return setHeaders(Arrays.asList(headers));
  }

  /**
   * Set one or more allowed headers. Possible values are a header name or <code>*</code> if any
   * header is allowed.
   *
   * @param headers Headers to set.
   * @return This cors.
   */
  public Cors setHeaders(final List<String> headers) {
    this.headers = allMatch(headers);
    return this;
  }

  /**
   * @return List of exposed headers.
   */
  public List<String> getExposedHeaders() {
    return exposedHeaders;
  }

  /**
   * Set the list of exposed headers.
   *
   * @param exposedHeaders Headers to expose.
   * @return This cors.
   */
  public Cors setExposedHeaders(final String... exposedHeaders) {
    return setExposedHeaders(Arrays.asList(exposedHeaders));
  }

  /**
   * Set the list of exposed headers.
   *
   * @param exposedHeaders Headers to expose.
   * @return This cors.
   */
  public Cors setExposedHeaders(final List<String> exposedHeaders) {
    this.exposedHeaders = requireNonNull(exposedHeaders, "Exposed headers are required.");
    return this;
  }

  /**
   * @return Preflight max age. How many seconds a client can cache a preflight request.
   */
  public Duration getMaxAge() {
    return maxAge;
  }

  /**
   * Set the preflight max age header. That's how many seconds a client can cache a preflight
   * request.
   *
   * @param preflightMaxAge Number of seconds or <code>-1</code> to turn this off.
   * @return This cors.
   */
  public Cors setMaxAge(final Duration preflightMaxAge) {
    this.maxAge = preflightMaxAge;
    return this;
  }

  public static Cors from(Config conf) {
    Config cors = conf.hasPath("cors") ? conf.getConfig("cors") : conf;
    Cors options = new Cors();
    if (cors.hasPath("origin")) {
      options.setOrigin(list(cors.getAnyRef("origin")));
    }
    if (cors.hasPath("credentials")) {
      options.setUseCredentials(cors.getBoolean("credentials"));
    }
    if (cors.hasPath("methods")) {
      options.setMethods(list(cors.getAnyRef("methods")));
    }
    if (cors.hasPath("headers")) {
      options.setHeaders(list(cors.getAnyRef("headers")));
    }
    if (cors.hasPath("maxAge")) {
      options.setMaxAge(Duration.ofSeconds(cors.getDuration("maxAge", TimeUnit.SECONDS)));
    }
    if (cors.hasPath("exposedHeaders")) {
      options.setExposedHeaders(list(cors.getAnyRef("exposedHeaders")));
    }
    return options;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<String> list(final Object value) {
    return value instanceof List ? (List) value : Collections.singletonList(value.toString());
  }

  private static Matcher<List<String>> allMatch(final List<String> values) {
    Predicate<String> predicate = firstMatch(values);
    Predicate<List<String>> allmatch = it -> it.stream().allMatch(predicate);
    return new Matcher<>(values, allmatch);
  }

  private static Matcher<String> firstMatch(final List<String> values) {
    List<Pattern> patterns = values.stream()
        .map(Cors::rewrite)
        .collect(Collectors.toList());
    Predicate<String> predicate = it -> patterns.stream()
        .filter(pattern -> pattern.matcher(it).matches())
        .findFirst()
        .isPresent();

    return new Matcher<>(values, predicate);
  }

  private static Pattern rewrite(final String origin) {
    return Pattern.compile(origin.replace(".", "\\.").replace("*", ".*"), Pattern.CASE_INSENSITIVE);
  }

}
