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
package org.jooby.ehcache;

import static java.util.Objects.requireNonNull;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.ehcache.CacheConfigurationBuilder;
import org.jooby.internal.ehcache.ConfigurationBuilder;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.Configuration;

/**
 * <h1>ehcache module</h1>
 * <p>
 * Provides advanced cache features via <a href="http://ehcache.org">Ehcache</a>
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>A {@link CacheManager}</li>
 * <li>Direct access to {@link Ehcache} instances</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Eh());
 *
 *   get("/", req {@literal ->} {
 *     CacheManager cm = req.require(CacheManager.class);
 *     // work with cm
 *
 *     Ehcache ehcache = req.require(Ehcache.class);
 *     // work with ehcache
 *   });
 * }
 * </pre>
 * <p>
 * <a href="http://ehcache.org">Ehcache</a> can be fully configured from your <code>.conf</code>
 * file and/or programmatically but for the time being there is no support for <code>xml</code>.
 * </p>
 *
 * <h2>caches</h2>
 * <p>
 * Caches are configured via <code>.conf</code> like almost everything in {@link Jooby}.
 * </p>
 *
 * <pre>
 * ehcache.cache.mycache {
 *   eternal = true
 * }
 * </pre>
 *
 * Later, we can access to <code>mycache</code> with:
 *
 * <pre>
 * {
 *   get("/", req {@literal ->} {
 *     Ehcache mycache = req.require(Ehcache.class);
 *   });
 * }
 * </pre>
 *
 * <h3>multiple caches</h3>
 * <p>
 * Multiple caches are also possible:
 * </p>
 *
 * <pre>
 * ehcache.cache.cache1 {
 *   maxEntriesLocalHeap = 100
 *   eternal = true
 * }
 *
 * ehcache.cache.cache2 {
 *   maxEntriesLocalHeap = 100
 *   eternal = true
 * }
 * </pre>
 *
 * Later, we can access to our caches with:
 *
 * <pre>
 * {
 *   get("/", req {@literal ->} {
 *     Ehcache cache1 = req.require("cache1", Ehcache.class);
 *     // ..
 *     Ehcache cache2 = req.require("cache2", Ehcache.class);
 *     // ..
 *   });
 * }
 * </pre>
 *
 * <h3>cache inheritance</h3>
 * <p>
 * Previous examples, show how to configure two or more caches, but it is also possible to inherit
 * cache configuration using the <code>default</code> cache:
 * </p>
 *
 * <pre>
 *
 * ehcache.cache.default {
 *   maxEntriesLocalHeap = 100
 *   eternal = true
 * }
 *
 * ehcache.cache.cache1 {
 *   eternal = false
 * }
 *
 * ehcache.cache.cache2 {
 *   maxEntriesLocalHeap = 1000
 * }
 * </pre>
 *
 * <p>
 * Here <code>cache1</code> and <code>cache2</code> will inherited their properties from the
 * <code>default</code> cache.
 *
 * Please note the <code>default</code> cache works as a template and isn't a real/usable cache.
 * </p>
 *
 * <h2>session store</h2>
 * <p>
 * This module provides an {@link EhSessionStore}. In order to use the {@link EhSessionStore} all
 * you have to do is define a <code>session</code> cache:
 * </p>
 *
 * <pre>
 * ehcache.cache.session {
 *   # cache will expire after 30 minutes of inactivity
 *   timeToIdle = 30m
 * }
 * </pre>
 *
 * And then register the {@link EhSessionStore}:
 *
 * <pre>
 * {
 *   session(EhSessionStore.class);
 * }
 * </pre>
 *
 * <h2>configuration</h2>
 * <p>
 * Configuration is done in one of two ways: 1) via <code>.conf</code>; or 2)
 * <code>programmatically:</code>.
 * </p>
 *
 * <h3>via .conf file</h3>
 *
 * <pre>
 *
 * ehcache {
 *
 *   defaultTransactionTimeout = 1m
 *
 *   dynamicConfig = true
 *
 *   maxBytesLocalDisk = 1k
 *
 *   maxBytesLocalHeap = 1k
 *
 *   maxBytesLocalOffHeap = 1m
 *
 *   monitor = off
 *
 *   # just one event listener
 *   cacheManagerEventListenerFactory {
 *     class = MyCacheEventListenerFactory
 *     p1 = "v1"
 *     p2 = true
 *   }
 *
 *   # or multiple event listeners
 *   cacheManagerEventListenerFactory {
 *     listener1 {
 *       class = MyCacheEventListenerFactory1
 *       p1 = "v1"
 *       p2 = true
 *     }
 *     listener2 {
 *       class = MyCacheEventListenerFactory2
 *     }
 *   }
 *
 *   diskStore.path = ${application.tmpdir}${file.separator}ehcache
 *
 *   # etc...
 * }
 *
 * </pre>
 *
 * <h3>programmatically</h3>
 *
 * <pre>
 * {
 *   use(new Eh().doWith(conf {@literal ->} {
 *     conf.setDefaultTransactionTimeoutInSeconds(120);
 *     // etc...
 *   }));
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.6.0
 */
public class Eh implements Jooby.Module {

  /**
   * Configure callback.
   */
  private BiConsumer<Configuration, Config> configurer;

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {

    Config ehcache = config.getConfig("ehcache");

    Config caches = ehcache.getConfig("cache");

    Config defcache = caches.getConfig("default");

    Configuration ehconfig = new ConfigurationBuilder().build(ehcache);

    ehconfig.setDefaultCacheConfiguration(new CacheConfigurationBuilder("default").build(defcache));

    Config userCaches = caches.withoutPath("default");

    for (Entry<String, ConfigValue> userCache : userCaches.root().entrySet()) {
      ConfigValue value = userCache.getValue();
      String cname = userCache.getKey();
      Config ccache = ConfigFactory.empty();
      if (value instanceof ConfigObject) {
        ccache = ((ConfigObject) value).toConfig();
      }
      ehconfig.addCache(new CacheConfigurationBuilder(cname).build(ccache.withFallback(defcache)));
    }

    if (configurer != null) {
      configurer.accept(ehconfig, config);
    }

    CacheManager cm = CacheManager.newInstance(ehconfig);

    binder.bind(CacheManager.class).toInstance(cm);

    env.onStop(cm::shutdown);

    String[] names = cm.getCacheNames();
    if (names.length == 1) {
      // just one cache, bind it without name
      binder.bind(Ehcache.class).toInstance(cm.getEhcache(names[0]));
    }

    // now bind each cache using it's name
    for (String name : cm.getCacheNames()) {
      binder.bind(Ehcache.class).annotatedWith(Names.named(name)).toInstance(cm.getEhcache(name));
    }
  }

  /**
   * Configure callback to manipulate a {@link Configuration} programmatically.
   *
   * @param configurer A configure callback.
   * @return This {@link Eh} cache.
   */
  public Eh doWith(final BiConsumer<Configuration, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  /**
   * Configure callback to manipulate a {@link Configuration} programmatically.
   *
   * @param configurer A configure callback.
   * @return This {@link Eh} cache.
   */
  public Eh doWith(final Consumer<Configuration> configurer) {
    requireNonNull(configurer, "Configurer callback is required.");
    return doWith((ehconf, conf) -> configurer.accept(ehconf));
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "ehcache.conf");
  }

}
