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
package org.jooby.hbs;

import static java.util.Objects.requireNonNull;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;
import org.jooby.internal.hbs.ConfigValueResolver;
import org.jooby.internal.hbs.HbsEngine;
import org.jooby.internal.hbs.HbsHelpers;
import org.jooby.internal.hbs.RequestValueResolver;
import org.jooby.internal.hbs.SessionValueResolver;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * Exposes a {@link Handlebars} and a {@link Renderer}.
 *
 * <h1>usage</h1>
 * <p>
 * It is pretty straightforward:
 * </p>
 *
 * <pre>
 * {
 *   use(new Hbs());
 *
 *   get("/", req {@literal ->} Results.html("index").put("model", new MyModel());
 * }
 * </pre>
 * <p>
 * public/index.html:
 * </p>
 *
 * <pre>
 *   {{model}}
 * </pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must end with: <code>.html</code>
 * file extension.
 * </p>
 *
 * <h1>helpers</h1>
 * <p>
 * Simple/basic helpers are add it at startup time:
 * </p>
 *
 * <pre>
 * {
 *   use(new Hbs().doWith((hbs, config) {@literal ->} {
 *     hbs.registerHelper("myhelper", (ctx, options) {@literal ->} {
 *       return ...;
 *     });
 *     hbs.registerHelpers(Helpers.class);
 *   });
 * }
 * </pre>
 * <p>
 * Now, if the helper depends on a service and require injection:
 * </p>
 *
 * <pre>
 * {
 *   use(new Hbs().with(Helpers.class));
 * }
 * </pre>
 *
 * <p>
 * The <code>Helpers</code> will be injected by Guice and Handlebars will scan and discover any
 * helper method.
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
 *   use(new Hbs("/", ".hbs"));
 * }
 * </pre>
 *
 * <h1>cache</h1>
 * <p>
 * Cache is OFF when <code>env=dev</code> (useful for template reloading), otherwise is ON.
 * </p>
 * <p>
 * Cache is backed by Guava and the default cache will expire after <code>100</code> entries.
 * </p>
 * <p>
 * If <code>100</code> entries is not enough or you need a more advanced cache setting, just set the
 * <code>hbs.cache</code> option:
 * </p>
 *
 * <pre>
 * hbs.cache = "expireAfterWrite=1h"
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
public class Hbs implements Jooby.Module {

  private final Handlebars hbs;

  private BiConsumer<Handlebars, Config> configurer;

  private Set<Class<?>> helpers = new HashSet<>();

  private Deque<ValueResolver> resolvers = new LinkedList<>();

  public Hbs(final String prefix, final String suffix, final Class<?>... helpers) {
    this.hbs = new Handlebars(new ClassPathTemplateLoader(prefix, suffix));
    with(helpers);
    // default value resolvers.
    this.resolvers.add(MapValueResolver.INSTANCE);
    this.resolvers.add(JavaBeanValueResolver.INSTANCE);
    this.resolvers.add(MethodValueResolver.INSTANCE);
    this.resolvers.add(new RequestValueResolver());
    this.resolvers.add(new SessionValueResolver());
    this.resolvers.add(new ConfigValueResolver());
    this.resolvers.add(FieldValueResolver.INSTANCE);
  }

  public Hbs(final String prefix, final Class<?>... helpers) {
    this(prefix, ".html", helpers);
  }

  public Hbs(final Class<?>... helpers) {
    this("/", helpers);
  }

  public Hbs doWith(final BiConsumer<Handlebars, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer is required.");
    return this;
  }

  public Hbs with(final Class<?>... helper) {
    for (Class<?> h : helper) {
      helpers.add(h);
    }
    return this;
  }

  public Hbs with(final ValueResolver resolver) {
    requireNonNull(resolver, "Value resolver is required.");
    this.resolvers.addFirst(resolver);
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {

    // cache
    if ("dev".equals(env.name()) || config.getString("hbs.cache").isEmpty()) {
      // noop cache
      hbs.with(NullTemplateCache.INSTANCE);
    } else {
      hbs.with(new GuavaTemplateCache(
          CacheBuilder
              .from(config.getString("hbs.cache"))
              .build()));
    }

    if (configurer != null) {
      configurer.accept(hbs, config);
    }

    /** XSS */
    hbs.registerHelper("xss", (value, opts) -> {
      String[] xss = new String[opts.params.length];
      System.arraycopy(opts.params, 0, xss, 0, opts.params.length);
      return new Handlebars.SafeString(env.xss(xss).apply(value.toString()));
    });

    binder.bind(Handlebars.class).toInstance(hbs);

    Multibinder<Object> helpersBinding = Multibinder
        .newSetBinder(binder, Object.class, Names.named("hbs.helpers"));
    helpers.forEach(h -> helpersBinding.addBinding().to(h));

    HbsEngine engine = new HbsEngine(hbs, resolvers.toArray(new ValueResolver[resolvers.size()]));

    Multibinder.newSetBinder(binder, Renderer.class).addBinding()
        .toInstance(engine);

    // helper bootstrap
    binder.bind(HbsHelpers.class).asEagerSingleton();
  }

  @Override
  public Config config() {
    return ConfigFactory.empty(Hbs.class.getName())
        .withValue("hbs.cache", ConfigValueFactory.fromAnyRef("maximumSize=100"));
  }

}
