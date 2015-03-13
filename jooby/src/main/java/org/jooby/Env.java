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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.jooby.util.ExSupplier;
import org.jooby.util.Switch;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.typesafe.config.Config;

/**
 * Allows to optimize, customize or apply defaults values for services.
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
 * By default env is set to dev, but you can change it by setting the <code>application.env</code>
 * property to anything else.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Env {

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
     * @param config A config instance.
     * @return A new environment.
     */
    Env build(@Nonnull Config config);

  }

  /**
   * Default builder.
   */
  Env.Builder DEFAULT = new Builder() {

    @Override
    public Env build(final Config config) {
      requireNonNull(config, "A config is required.");
      String name = config.hasPath("application.env") ? config.getString("application.env") : "dev";
      return new Env() {

        @Override
        public String name() {
          return name;
        }

        @Override
        public Config config() {
          return config;
        }

        @Override
        public String toString() {
          return name();
        }
      };
    }
  };

  /**
   * @return Env's name.
   */
  @Nonnull
  String name();

  /**
   * @return environment properties.
   */
  Config config();

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
    return resolve(text, config());
  }

  /**
   * Returns a string with all substitutions (the <code>${foo.bar}</code> syntax,
   * see <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">the
   * spec</a>) resolved. Substitutions are looked up using the {@link #config()} as the root object,
   * that is, a substitution <code>${foo.bar}</code> will be replaced with
   * the result of <code>getValue("foo.bar")</code>.
   *
   * @param text Text to process.
   * @param startDelimiter Start delimiter.
   * @param endDelimiter End delimiter.
   * @return A processed string.
   */
  default String resolve(final String text, final String startDelimiter, final String endDelimiter) {
    return resolve(text, config(), startDelimiter, endDelimiter);
  }

  /**
   * Returns a string with all substitutions (the <code>${foo.bar}</code> syntax,
   * see <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">the
   * spec</a>) resolved. Substitutions are looked up using the <code>source</code> param as the
   * root object, that is, a substitution <code>${foo.bar}</code> will be replaced with
   * the result of <code>getValue("foo.bar")</code>.
   *
   * @param text Text to process.
   * @param source The source config to use.
   * @return A processed string.
   */
  default @Nonnull String resolve(@Nonnull final String text, @Nonnull final Config source) {
    return resolve(text, source, "${", "}");
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
  default @Nonnull String resolve(@Nonnull final String text, @Nonnull final Config source,
      @Nonnull final String startDelimiter, @Nonnull final String endDelimiter) {
    requireNonNull(text, "Text is required.");
    requireNonNull(source, "A config source is required.");
    checkArgument(!Strings.isNullOrEmpty(startDelimiter), "Start delimiter is required.");
    checkArgument(!Strings.isNullOrEmpty(endDelimiter), "End delimiter is required.");
    if (text.length() == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    int offset = 0;
    int start = text.indexOf(startDelimiter);
    while (start >= 0) {
      int end = text.indexOf(endDelimiter, start + startDelimiter.length());
      if (end == -1) {
        throw new IllegalArgumentException("Unclosed placeholder: "
            + Splitter.on(CharMatcher.WHITESPACE)
                .split(text.subSequence(start, text.length())).iterator().next());
      }
      buffer.append(text.substring(offset, start));
      String key = text.substring(start + startDelimiter.length(), end);
      buffer.append(source.getAnyRef(key));
      offset = end + endDelimiter.length();
      start = text.indexOf(startDelimiter, offset);
    }
    if (buffer.length() == 0) {
      return text;
    }
    if (offset < text.length()) {
      buffer.append(text.substring(offset));
    }
    return buffer.toString();
  }

  /**
   * Runs the callback function if the current env matches the given name.
   *
   * @param name A name to test for.
   * @param fn A callback function.
   * @param <T> A resulting type.
   * @return A resulting object.
   * @throws Exception If something fails.
   */
  default <T> Optional<T> ifMode(@Nonnull final String name, @Nonnull final ExSupplier<T> fn)
      throws Exception {
    return when(name, fn).value();
  }

  /**
   * Produces a {@link Switch} of the current {@link Env}.
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
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final String name, @Nonnull final ExSupplier<T> fn) {
    return Switch.<T> newSwitch(name()).when(name, fn);
  }

  /**
   * Produces a {@link Switch} of the current {@link Env}.
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
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final String name, @Nonnull final T result) {
    return Switch.<T> newSwitch(name()).when(name, result);
  }

  /**
   * Produces a {@link Switch} of the current {@link Env}.
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
   * @return A new switch.
   */
  @Nonnull
  default <T> Switch<String, T> when(@Nonnull final Predicate<String> predicate,
      @Nonnull final T result) {
    return Switch.<T> newSwitch(name()).when(predicate, result);
  }
}
