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
package org.jooby.pebble;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>pebble</h1>
 * <p>
 * <a href="http://www.mitchellbosecke.com/pebble">Pebble</a> a lightweight but rock solid Java
 * templating engine.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>
 * {
 *   use(new Pebble());
 *
 *   get("/", req {@literal ->} Results.html("index").put("model", new MyModel());
 *
 *   // or Pebble API
 *   get("/pebble-api", req {@literal ->} {
 *     PebbleEngine pebble = req.require(PebbleEngine.class);
 *     PebbleTemplate template = pebble.getTemplate("template");
 *     template.evaluate(...);
 *   });
 * }
 * </pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must end with: <code>.html</code>
 * file extension.
 * </p>
 *
 * <h2>template loader</h2>
 * <p>
 * Templates are loaded from the root of classpath and must end with <code>.html</code>. You can
 * change the default template location and extensions too:
 * </p>
 *
 * <pre>
 * {
 *   use(new Pebble("templates", ".pebble"));
 * }
 * </pre>
 *
 * <h2>template cache</h2>
 * <p>
 * Cache is OFF when <code>env=dev</code> (useful for template reloading), otherwise is ON.
 * </p>
 * <p>
 * Cache is backed by Guava and the default cache will expire after <code>200</code> entries.
 * </p>
 * <p>
 * If <code>200</code> entries is not enough or you need a more advanced cache setting, just set the
 * <code>pebble.cache</code> option:
 * </p>
 *
 * <pre>
 * pebble.cache = "expireAfterWrite=1h;maximumSize=200"
 * </pre>
 *
 * <p>
 * See {@link CacheBuilderSpec}.
 * </p>
 *
 * <h2>tag cache</h2>
 * <p>
 * It works like template cache, except the cache is controlled by the property:
 * <code>pebble.tagCache</code>
 * </p>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * Advanced configuration if provided by callback:
 * </p>
 *
 * <pre>
 * {
 *   use(new Pebble().doWith(pebble {@literal ->} {
 *     pebble.extension(...);
 *     pebble.loader(...);
 *   }));
 * }
 * </pre>
 *
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @author edgar
 * @since 0.13.0
 */
public class Pebble implements Jooby.Module {

  private BiConsumer<PebbleEngine.Builder, Config> callback;

  private PebbleEngine.Builder pebble;

  /**
   * Creates a new {@link Pebble} module. Add a {@link ClasspathLoader} and set prefix and suffix on
   * it.
   *
   * @param prefix Template prefix location (might be null).
   * @param suffix Template extension.
   */
  public Pebble(final String prefix, final String suffix) {
    this.pebble = new PebbleEngine.Builder().loader(loader(prefix, suffix));
  }

  /**
   * Creates a new {@link Pebble} module. Add a {@link ClasspathLoader} that loads template from
   * root of classpath and ends with the given <code>suffix</code>.
   *
   * @param suffix Template extension.
   */
  public Pebble(final String suffix) {
    this(null, suffix);
  }

  /**
   * Creates a new {@link Pebble} module. Add a {@link ClasspathLoader} that loads template from
   * root of classpath and ends with the given <code>.html</code>.
   */
  public Pebble() {
    this(null, ".html");
  }

  /**
   * Advanced configuration callback for {@link PebbleEngine.Builder}.
   *
   * @param callback A callback to finish setup.
   * @return This module.
   */
  public Pebble doWith(final BiConsumer<PebbleEngine.Builder, Config> callback) {
    this.callback = requireNonNull(callback, "Callback is required.");
    return this;
  }

  /**
   * Advanced configuration callback for {@link PebbleEngine.Builder}.
   *
   * @param callback A callback to finish setup.
   * @return This module.
   */
  public Pebble doWith(final Consumer<PebbleEngine.Builder> callback) {
    requireNonNull(callback, "Callback is required.");
    return doWith((p, c) -> callback.accept(p));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    /** Template cache. */
    String mode = env.name();
	pebble.cacheActive(!mode.equals("dev"));
    if (mode.equals("dev") || conf.getString("pebble.cache").isEmpty()) {
      pebble.templateCache(null);
    } else {
      pebble.templateCache(CacheBuilder.from(conf.getString("pebble.cache")).build());
    }

    /** Tag cache. */
    if (mode.equals("dev") || conf.getString("pebble.tagCache").isEmpty()) {
      pebble.tagCache(null);
    } else {
      pebble.tagCache(CacheBuilder.from(conf.getString("pebble.tagCache")).build());
    }

    /** locale. */
    pebble.defaultLocale(env.locale());

    // done defaults, allow user to override everything
    if (callback != null) {
      callback.accept(pebble, conf);
    }

    PebbleEngine pebble = this.pebble.build();
    binder.bind(PebbleEngine.class).toInstance(pebble);

    Multibinder.newSetBinder(binder, Renderer.class)
        .addBinding()
        .toInstance(new PebbleRenderer(pebble));
  }

  @Override
  public Config config() {
    return ConfigFactory.empty(Pebble.class.getName())
        .withValue("pebble.cache", ConfigValueFactory.fromAnyRef("maximumSize=200"))
        .withValue("pebble.tagCache", ConfigValueFactory.fromAnyRef("maximumSize=200"));
  }

  private static Loader<String> loader(final String prefix, final String suffix) {
    Loader<String> loader = new ClasspathLoader();
    loader.setPrefix(prefix);
    loader.setSuffix(suffix);
    return loader;
  }

}
