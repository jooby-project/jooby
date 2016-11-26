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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.API;
import javaslang.control.Option;
import javaslang.control.Try.CheckedConsumer;

/**
 * Allows to optimize, customize or apply defaults values for application services.
 *
 * <p>
 * A env is represented by it's name. For example: <code>dev</code>, <code>prod</code>, etc... A
 * <strong>dev</strong> env is special and a module provider could do some special configuration for
 * development, like turning off a cache, reloading of resources, etc.
 * </p>
 * <p>
 * Same is true for not <strong>dev</strong> environments. For example, a module provider might
 * create a high performance connection pool, caches, etc.
 * </p>
 * <p>
 * By default env is set to <code>dev</code>, but you can change it by setting the
 * <code>application.env</code> property to anything else.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Env extends LifeCycle {

  /**
   * Template literal implementation, replaces <code>${expression}</code> from a String using a
   * {@link Config} object.
   *
   * @author edgar
   */
  class Resolver {
    private String startDelim = "${";

    private String endDelim = "}";

    private Config source;

    private boolean ignoreMissing;

    /**
     * Set property source.
     *
     * @param source Source.
     * @return This resolver.
     */
    public Resolver source(final Map<String, Object> source) {
      this.source = ConfigFactory.parseMap(source);
      return this;
    }

    /**
     * Set property source.
     *
     * @param source Source.
     * @return This resolver.
     */
    public Resolver source(final Config source) {
      this.source = source;
      return this;
    }

    /**
     * Set start and end delimiters.
     *
     * @param start Start delimiter.
     * @param end End delimiter.
     * @return This resolver.
     */
    public Resolver delimiters(final String start, final String end) {
      this.startDelim = requireNonNull(start, "Start delimiter required.");
      this.endDelim = requireNonNull(end, "End delmiter required.");
      return this;
    }

    /**
     * Ignore missing property replacement and leave the expression untouch.
     *
     * @return This resolver.
     */
    public Resolver ignoreMissing() {
      this.ignoreMissing = true;
      return this;
    }

    /**
     * Returns a string with all substitutions (the <code>${foo.bar}</code> syntax,
     * see <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">the
     * spec</a>) resolved. Substitutions are looked up using the <code>source</code> param as the
     * root object, that is, a substitution <code>${foo.bar}</code> will be replaced with
     * the result of <code>getValue("foo.bar")</code>.
     *
     * @param text Text to process.
     * @param source The source config to use
     * @param startDelimiter Start delimiter.
     * @param endDelimiter End delimiter.
     * @return A processed string.
     */
    public String resolve(final String text) {
      requireNonNull(text, "Text is required.");
      if (text.length() == 0) {
        return "";
      }

      BiFunction<Integer, BiFunction<Integer, Integer, RuntimeException>, RuntimeException> err = (
          start, ex) -> {
        String snapshot = text.substring(0, start);
        int line = Splitter.on('\n').splitToList(snapshot).size();
        int column = start - snapshot.lastIndexOf('\n');
        return ex.apply(line, column);
      };

      StringBuilder buffer = new StringBuilder();
      int offset = 0;
      int start = text.indexOf(startDelim);
      while (start >= 0) {
        int end = text.indexOf(endDelim, start + startDelim.length());
        if (end == -1) {
          throw err.apply(start, (line, column) -> new IllegalArgumentException(
              "found '" + startDelim + "' expecting '" + endDelim + "' at " + line + ":"
                  + column));
        }
        buffer.append(text.substring(offset, start));
        String key = text.substring(start + startDelim.length(), end);
        Object value;
        if (source.hasPath(key)) {
          value = source.getAnyRef(key);
        } else {
          if (ignoreMissing) {
            value = text.substring(start, end + endDelim.length());
          } else {
            throw err.apply(start, (line, column) -> new NoSuchElementException(
                "No configuration setting found for key '" + key + "' at " + line + ":" + column));
          }
        }
        buffer.append(value);
        offset = end + endDelim.length();
        start = text.indexOf(startDelim, offset);
      }
      if (buffer.length() == 0) {
        return text;
      }
      if (offset < text.length()) {
        buffer.append(text.substring(offset));
      }
      return buffer.toString();
    }
  }

  /**
   * Utility class for generating {@link Key} for named services.
   *
   * @author edgar
   */
  class ServiceKey {
    private Map<Object, Integer> instances = new HashMap<>();

    /**
     * Generate at least one named key for the provided type. If this is the first call for the
     * provided type then it generates an unnamed key.
     *
     * @param type Service type.
     * @param name Service name.
     * @param keys Key callback. Invoked once with a named key, and optionally again with an unamed
     *        key.
     * @param <T> Service type.
     */
    public <T> void generate(final Class<T> type, final String name, final Consumer<Key<T>> keys) {
      Integer c = instances.put(type, instances.getOrDefault(type, 0) + 1);
      if (c == null) {
        // def key
        keys.accept(Key.get(type));
      }
      keys.accept(Key.get(type, Names.named(name)));
    }
  }

  /**
   * Build an jooby environment.
   *
   * @author edgar
   */
  interface Builder {

    /**
     * Build a new environment from a {@link Config} object. The environment is created from the
     * <code>application.env</code> property. If such property is missing, env's name must be:
     * <code>dev</code>.
     *
     * Please note an environment created with this method won't have a {@link Env#router()}.
     *
     * @param config A config instance.
     * @return A new environment.
     */
    default Env build(final Config config) {
      return build(config, null, Locale.getDefault());
    }

    /**
     * Build a new environment from a {@link Config} object. The environment is created from the
     * <code>application.env</code> property. If such property is missing, env's name must be:
     * <code>dev</code>.
     *
     * @param config A config instance.
     * @param router Application router.
     * @param locale App locale.
     * @return A new environment.
     */
    Env build(Config config, Router router, Locale locale);
  }

  /**
   * Default builder.
   */
  Env.Builder DEFAULT = (config, router, locale) -> {
    requireNonNull(config, "Config required.");
    String name = config.hasPath("application.env") ? config.getString("application.env") : "dev";
    return new Env() {

      private ImmutableList.Builder<CheckedConsumer<Registry>> start = ImmutableList.builder();

      private ImmutableList.Builder<CheckedConsumer<Registry>> shutdown = ImmutableList.builder();

      private Map<String, Function<String, String>> xss = new HashMap<>();

      private ServiceKey key = new ServiceKey();

      @Override
      public String name() {
        return name;
      }

      @Override
      public ServiceKey serviceKey() {
        return key;
      }

      @Override
      public Router router() {
        if (router == null) {
          throw new UnsupportedOperationException();
        }
        return router;
      }

      @Override
      public Config config() {
        return config;
      }

      @Override
      public Locale locale() {
        return locale;
      }

      @Override
      public String toString() {
        return name();
      }

      @Override
      public List<CheckedConsumer<Registry>> stopTasks() {
        return shutdown.build();
      }

      @Override
      public Env onStop(final CheckedConsumer<Registry> task) {
        this.shutdown.add(task);
        return this;
      }

      @Override
      public Env onStart(final CheckedConsumer<Registry> task) {
        this.start.add(task);
        return this;
      }

      @Override
      public List<CheckedConsumer<Registry>> startTasks() {
        return this.start.build();
      }

      @Override
      public Map<String, Function<String, String>> xss() {
        return Collections.unmodifiableMap(xss);
      }

      @Override
      public Env xss(final String name, final Function<String, String> escaper) {
        xss.put(requireNonNull(name, "Name required."),
            requireNonNull(escaper, "Function required."));
        return this;
      }
    };
  };

  /**
   * @return Env's name.
   */
  String name();

  /**
   * Application router.
   *
   * @return Available {@link Router}.
   * @throws UnsupportedOperationException if router isn't available.
   */
  Router router() throws UnsupportedOperationException;

  /**
   * @return environment properties.
   */
  Config config();

  /**
   * @return Default locale from <code>application.lang</code>.
   */
  Locale locale();

  /**
   * @return Utility method for generating keys for named services.
   */
  default ServiceKey serviceKey() {
    return new ServiceKey();
  }

  /**
   * Returns a string with all substitutions (the <code>${foo.bar}</code> syntax,
   * see <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">the
   * spec</a>) resolved. Substitutions are looked up using the {@link #config()} as the root object,
   * that is, a substitution <code>${foo.bar}</code> will be replaced with
   * the result of <code>getValue("foo.bar")</code>.
   *
   * @param text Text to process.
   * @return A processed string.
   */
  default String resolve(final String text) {
    return resolver().resolve(text);
  }

  /**
   * Creates a new environment {@link Resolver}.
   *
   * @return
   */
  default Resolver resolver() {
    return new Resolver()
        .source(config());
  }

  /**
   * Runs the callback function if the current env matches the given name.
   *
   * @param name A name to test for.
   * @param fn A callback function.
   * @param <T> A resulting type.
   * @return A resulting object.
   */
  default <T> Optional<T> ifMode(final String name, final Supplier<T> fn) {
    if (name().equals(name)) {
      return Optional.of(fn.get());
    }
    return Optional.empty();
  }

  /**
   * Produces a {@link API.Match} of the current {@link Env}.
   *
   * <pre>
   *   String accessKey = env.match()
   *                          .when("dev", () {@literal ->} "1234")
   *                          .when("stage", () {@literal ->} "4321")
   *                          .when("prod", () {@literal ->} "abc")
   *                          .get();
   * </pre>
   *
   * @return A new matcher.
   */
  default API.Match<String> match() {
    return API.Match(name());
  }

  /**
   * Produces a {@link API.Match} of the current {@link Env}.
   *
   * <pre>
   *   String accessKey = env.when("dev", () {@literal ->} "1234")
   *                          .when("stage", () {@literal ->} "4321")
   *                          .when("prod", () {@literal ->} "abc")
   *                          .get();
   * </pre>
   *
   * @param name A name to test for.
   * @param fn A callback function.
   * @param <T> A resulting type.
   * @return A new matcher.
   */
  default <T> Option<T> when(final String name, final Supplier<T> fn) {
    return match().option(API.Case(API.$(name), fn));
  }

  /**
   * Produces a {@link API.Match} of the current {@link Env}.
   *
   * <pre>
   *   String accessKey = env.when("dev", "1234")
   *                          .when("stage", "4321")
   *                          .when("prod", "abc")
   *                          .get();
   * </pre>
   *
   * @param name A name to test for.
   * @param result A constant value to return.
   * @param <T> A resulting type.
   * @return A new matcher.
   */
  default <T> Option<T> when(final String name, final T result) {
    return match().option(API.Case(API.$(name), result));
  }

  /**
   * Produces a {@link API.Match} of the current {@link Env}.
   *
   * <pre>
   *   String accessKey = env.when("dev", () {@literal ->} "1234")
   *                          .when("stage", () {@literal ->} "4321")
   *                          .when("prod", () {@literal ->} "abc")
   *                          .get();
   * </pre>
   *
   * @param predicate A predicate to use.
   * @param result A constant value to return.
   * @param <T> A resulting type.
   * @return A new matcher.
   */
  default <T> Option<T> when(final Predicate<String> predicate, final T result) {
    return match().option(API.Case(predicate, result));
  }

  /**
   * @return XSS escape functions.
   */
  Map<String, Function<String, String>> xss();

  /**
   * Get or chain the required xss functions.
   *
   * @param xss XSS to combine.
   * @return Chain of required xss functions.
   */
  default Function<String, String> xss(final String... xss) {
    Map<String, Function<String, String>> fn = xss();
    BinaryOperator<Function<String, String>> reduce = Function::andThen;
    return Arrays.asList(xss)
        .stream()
        .map(fn::get)
        .filter(Objects::nonNull)
        .reduce(Function.identity(), reduce);
  }

  /**
   * Set/override a XSS escape function.
   *
   * @param name Escape's name.
   * @param escaper Escape function.
   * @return This environment.
   */
  Env xss(String name, Function<String, String> escaper);

  /**
   * @return List of start tasks.
   */
  List<CheckedConsumer<Registry>> startTasks();

  /**
   * @return List of stop tasks.
   */
  List<CheckedConsumer<Registry>> stopTasks();

}
