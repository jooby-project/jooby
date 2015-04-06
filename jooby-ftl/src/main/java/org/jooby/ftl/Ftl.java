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
package org.jooby.ftl;

import static java.util.Objects.requireNonNull;

import java.util.Properties;
import java.util.function.BiConsumer;

import org.jooby.Env;
import org.jooby.BodyFormatter;
import org.jooby.Jooby;
import org.jooby.View;
import org.jooby.internal.ftl.Engine;
import org.jooby.internal.ftl.GuavaCacheStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Exposes a {@link Configuration} and a {@link BodyFormatter}.
 *
 * <h1>usage</h1>
 * <p>
 * It is pretty straightforward:
 * </p>
 *
 * <pre>
 * {
 *   use(new Ftl());
 *
 *   get("/", req {@literal ->} Results.html("index").put("model", new MyModel());
 * }
 * </pre>
 * <p>
 * public/index.html:
 * </p>
 *
 * <pre>
 *   ${model}
 * </pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must end with: <code>.html</code>
 * file extension.
 * </p>
 *
 * <h1>configuration</h1>
 * <p>
 * There are two ways of changing a Freemarker configuration:
 * </p>
 * <h2>application.conf</h2>
 * <p>
 * Just add a <code>freemarker.*</code> option to your <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 * freemarker.default_encoding = UTF-8
 * </pre>
 *
 * <h2>programmatically</h2>
 *
 * <pre>
 * {
 *   use(new Ftl().doWith((freemarker, config) {@literal ->} {
 *     freemarker.setDefaultEncoding("UTF-8");
 *   });
 * }
 * </pre>
 *
 * <p>
 * Keep in mind this is just an example and you don't need to set the default encoding. Default
 * encoding is set to: <code>application.charset</code> which is <code>UTF-8</code> by default.
 * </p>
 *
 * <h1>template loader</h1>
 * <p>
 * Templates are loaded from the root of classpath and must end with <code>.html</code>. You can
 * change the default template location and extensions too:
 * </p>
 *
 * <pre>
 * {
 *   use(new Ftl("/", ".ftl"));
 * }
 * </pre>
 *
 * <h1>cache</h1>
 * <p>
 * Cache is OFF when <code>env=dev</code> (useful for template reloading), otherwise is ON.
 * </p>
 * <p>
 * Cache is backed by Guava and default cache will expire after <code>100</code> entries.
 * </p>
 * <p>
 * If <code>100</code> entries is not enough or you need a more advanced cache setting, just set the
 * <code>freemarker.cache</code> option:
 * </p>
 *
 * <pre>
 * freemarker.cache = "expireAfterWrite=1h"
 * </pre>
 *
 * <p>
 * See {@link CacheBuilderSpec}.
 * </p>
 *
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
public class Ftl implements Jooby.Module {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String prefix;

  private final String suffix;

  private BiConsumer<Configuration, Config> configurer;

  public Ftl(final String prefix, final String suffix) {
    this.prefix = requireNonNull(prefix, "Template prefix is required.");
    this.suffix = requireNonNull(suffix, "Template suffix is required.");
  }

  public Ftl(final String prefix) {
    this(prefix, ".html");
  }

  public Ftl() {
    this("/");
  }

  public Ftl doWith(final BiConsumer<Configuration, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    try {
      Configuration freemarker = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
      log.debug("Freemarker: {}", Configuration.getVersion());
      freemarker.setSettings(properties(config));
      freemarker.setTemplateLoader(new ClassTemplateLoader(getClass().getClassLoader(), prefix));

      // cache
      if ("dev".equals(env.name()) || config.getString("freemarker.cache").isEmpty()) {
        // noop cache
        freemarker.setCacheStorage(NullCacheStorage.INSTANCE);
      } else {
        freemarker.setCacheStorage(
            new GuavaCacheStorage(
                CacheBuilder
                    .from(config.getString("freemarker.cache"))
                    .build()
            ));
      }

      if (configurer != null) {
        configurer.accept(freemarker, config);
      }

      binder.bind(Configuration.class).toInstance(freemarker);

      Engine engine = new Engine(freemarker, prefix, suffix);

      Multibinder.newSetBinder(binder, BodyFormatter.class)
          .addBinding().toInstance(engine);

      // direct access
      binder.bind(Key.get(View.Engine.class, Names.named(engine.name()))).toInstance(engine);
    } catch (TemplateException ex) {
      throw new IllegalStateException("Freemarker configuration results in error", ex);
    }
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "freemarker.conf");
  }

  private Properties properties(final Config config) {
    Properties props = new Properties();

    // dump
    config.getConfig("freemarker").entrySet().forEach(e -> {
      String name = e.getKey();
      String value = e.getValue().unwrapped().toString();
      log.debug("  freemarker.{} = {}", name, value);
      props.setProperty(name, value);
    });
    // this is a jooby option
    props.remove("cache");

    return props;
  }

}
