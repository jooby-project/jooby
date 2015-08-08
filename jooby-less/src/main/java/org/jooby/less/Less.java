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
package org.jooby.less;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.internal.less.ForwardingLessCompiler;
import org.jooby.internal.less.LessHandler;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.LessCompiler.SourceMapConfiguration;
import com.github.sommeri.less4j.core.DefaultLessCompiler;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>less css pre-processor</h1>
 * <p>
 * Transform <code>less</code> files to <code>css</code> via <a
 * href="https://github.com/SomMeri/less4j">less4j</a>.
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>A Less handler</li>
 * <li>A Thread-Safe instance of {@link LessCompiler}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Less("/css/**"));
 * }
 * </pre>
 *
 * styles.css:
 *
 * <pre>
 * &#64;font-stack: Helvetica, sans-serif;
 * &#64;primary-color: #333;
 *
 * body {
 *   font: &#64;font-stack;
 *   color: &#64;primary-color;
 * }
 * </pre>
 *
 * A request like:
 *
 * <pre>
 * GET /css/style.css
 * </pre>
 *
 * or
 *
 * <pre>
 * GET /css/style.less
 * </pre>
 *
 * Produces:
 *
 * <pre>
 * body {
 *   font: Helvetica, sans-serif;
 *   color: #333;
 * }
 * </pre>
 *
 * <h2>configuration</h2>
 * <p>
 * A {@link Configuration} object can be configured via <code>.conf</code> file and/or
 * programmatically via {@link #doWith(Consumer)}.
 * </p>
 *
 * application.conf:
 *
 * <pre>
 * less.compressing = true
 * </pre>
 *
 * or
 *
 * <pre>
 * {
 *   use(new Less("/css/**").doWith(conf {@literal ->} {
 *     conf.setCompressing(true);
 *   }));
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.9.2
 */
public class Less implements Jooby.Module {

  private String pattern;

  private String location;

  private Consumer<Configuration> configurer;

  /**
   * Creates a new {@link Less} module.
   *
   * @param pattern A route pattern. Used it to matches request against less resources.
   * @param location A location pattern. Used to locate less resources.
   */
  public Less(final String pattern, final String location) {
    this.pattern = requireNonNull(pattern, "Pattern is required.");
    this.location = requireNonNull(location, "Location pattern is required.");
  }

  /**
   * Creates a new {@link Less} module. Location pattern is set to <code>/</code>.
   *
   * @param pattern A route pattern. Used it to matches request against less resources.
   */
  public Less(final String pattern) {
    this(pattern, "/");
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Configuration options = options(env.name().equals("dev"), conf.getConfig("less"));

    if (configurer != null) {
      configurer.accept(options);
    }

    LessCompiler compiler = new ForwardingLessCompiler(new DefaultLessCompiler(), options);
    binder.bind(LessCompiler.class).toInstance(compiler);

    LessHandler handler = new LessHandler(location, compiler);
    handler.cdn(conf.getString("assets.cdn"));
    handler.etag(conf.getBoolean("assets.etag"));

    Multibinder.newSetBinder(binder, Route.Definition.class)
        .addBinding()
        .toInstance(new Route.Definition("GET", pattern, handler));
  }

  /**
   * Hook to finish/overwrite a less configuration.
   *
   * @param configurer A configuration callback.
   * @return This module.
   */
  public Less doWith(final Consumer<Configuration> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");

    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "less.conf");
  }

  private Configuration options(final boolean dev, final Config conf) {
    Configuration configuration = new Configuration();

    boolean compressing = conf.hasPath("compressing")
        ? conf.getBoolean("compressing")
        : !dev;
    configuration.setCompressing(compressing);

    SourceMapConfiguration sourceMap = configuration.getSourceMapConfiguration();
    sourceMap.setEncodingCharset(conf.getString("sourceMap.encodingCharset"));
    sourceMap.setIncludeSourcesContent(conf.getBoolean("sourceMap.includeSourcesContent"));
    sourceMap.setInline(conf.getBoolean("sourceMap.inline"));
    sourceMap.setRelativizePaths(conf.getBoolean("sourceMap.relativizePaths"));
    boolean linkSourceMap = conf.hasPath("sourceMap.linkSourceMap")
        ? conf.getBoolean("sourceMap.linkSourceMap")
        : dev;
    sourceMap.setLinkSourceMap(linkSourceMap);

    return configuration;
  }

}
