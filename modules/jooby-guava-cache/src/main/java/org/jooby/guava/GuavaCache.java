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
package org.jooby.guava;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.LoadingCache;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>guava-cache</h1>
 * <p>
 * {@link Cache} services from <a href="https://github.com/google/guava">Guava</a>
 * </p>
 *
 * <h2>usage</h2>
 *
 * <p>
 * App.java:
 * </p>
 * <pre>
 * {
 *   use(GuavaCache.newCache());
 *
 *   get("/", req {@literal ->} {
 *     Cache cache = req.require(Cache.class);
 *     // do with cache...
 *   });
 * }
 * </pre>
 *
 * <h2>options</h2>
 *
 * <h3>cache configuration</h3>
 * <p>
 * A default cache will be created, if you need to control the number of entries, expire policy,
 * etc... set the <code>guava.cache</code> property in <code>application.conf</code>, like:
 * </p>
 *
 * <pre>
 * guava.cache {
 *   maximumSize = 10
 *   concurrencyLevel = 2
 * }
 * </pre>
 *
 * or via {@link CacheBuilderSpec} syntax:
 *
 * <pre>
 * guava.cache = "maximumSize=10,concurrencyLevel=2"
 * </pre>
 *
 * <h3>multiple caches</h3>
 * <p>
 * Just add entries to: <code>guava.</code>, like:
 * </p>
 *
 * <pre>
 * # default cache (don't need a name on require calls)
 * guava.cache = "maximumSize=10"
 *
 * guava.cache1 = "maximumSize=1"
 *
 * guava.cacheX = "maximumSize=100"
 * </pre>
 *
 * <pre>
 * {
 *   get("/", req {@literal ->} {
 *     Cache defcache = req.require(Cache.class);
 *
 *     Cache cache1 = req.require("cache1", Cache.class);
 *
 *     Cache cacheX = req.require("cacheX", Cache.class);
 *   });
 * }
 * </pre>
 *
 * <h3>type-safe caches</h3>
 * <p>
 * Type safe caches are provided by calling and creating a new {@link GuavaCache} subclass:
 * </p>
 *
 * <pre>
 * {
 *   // please notes the {} at the line of the next line
 *   use(new GuavaCache&lt;Integer, String&gt;() {});
 * }
 * </pre>
 *
 * <p>
 * Later, you can inject this cache in a type-safe manner:
 * </p>
 *
 * <pre>
 * &#64;Inject
 * public MyService(Cache&lt;Integer, String&gt; cache) {
 *  ...
 * }
 * </pre>
 *
 * <h2>session store</h2>
 * <p>
 * This module comes with a {@link Session.Store} implementation. In order to use it you need to
 * define a cache named <code>session</code> in your <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 * guava.session = "maximumSize=10"
 * </pre>
 *
 * And set the {@link GuavaSessionStore}:
 *
 * <pre>
 * {
 *   session(GuavaSessionStore.class);
 * }
 * </pre>
 *
 * You can access to the ```session``` via name:
 *
 * <pre>
 * {
 *   get("/", req {@literal} {
 *     Cache cache = req.require("session", Cache.class);
 *   });
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.15.0
 * @param <K> Cache key.
 * @param <V> Cache value.
 * @see GuavaSessionStore
 */
public abstract class GuavaCache<K, V> implements Jooby.Module {

  public interface Callback<K, V, C extends Cache<K, V>> {
    C apply(String name, CacheBuilder<K, V> builder);
  }

  private static final String DEF = "guava";

  private Callback<K, V, Cache<K, V>> callback = (n, b) -> b.build();

  private final String name;

  /**
   * Creates a new {@link GuavaCache} using the provided namespace.
   *
   * @param name Cache namespace.
   */
  public GuavaCache(final String name) {
    this.name = name;
  }

  /**
   * Creates a new {@link GuavaCache} using <code>guava</code> as cache namespace.
   */
  public GuavaCache() {
    this(DEF);
  }

  /**
   * Creates a new {@link GuavaCache} with {@link String} and {@link Object} types as key/value.
   *
   * @return A new {@link GuavaCache}.
   */
  public static final GuavaCache<String, Object> newCache() {
    return new GuavaCache<String, Object>() {
    };
  }

  /**
   * Configure a cache builder and creates a new {@link Cache}.
   *
   * @param configurer Configurer callback.
   * @return This instance.
   */
  public GuavaCache<K, V> doWith(final Callback<K, V, Cache<K, V>> configurer) {
    this.callback = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Config gconf = conf.hasPath(name) ? conf : ConfigFactory.empty();

    gconf = gconf.withFallback(
        ConfigFactory.empty().withValue("guava.cache", ConfigValueFactory.fromAnyRef("")));

    gconf.getObject(name).unwrapped().forEach((name, spec) -> {
      CacheBuilder<K, V> cb = (CacheBuilder<K, V>) CacheBuilder.from(toSpec(spec));

      Cache<K, V> cache = callback.apply(name, cb);

      List<TypeLiteral> types = cacheType(name, cache, getClass().getGenericSuperclass());

      types.forEach(type -> {
        binder.bind(Key.get(type, Names.named(name)))
            .toInstance(cache);
        if (name.equals("cache")) {
          // unnamed cache for default cache
          binder.bind(type).toInstance(cache);
          // raw type for default cache
          binder.bind(type.getRawType()).toInstance(cache);
        }
        if (name.equals("session")) {
          binder.bind(Key.get(type, Names.named(name))).toInstance(cache);
          binder.bind(Key.get(type.getRawType(), Names.named(name))).toInstance(cache);
        }
      });
    });
  }

  @SuppressWarnings("rawtypes")
  private static List<TypeLiteral> cacheType(final String name, final Cache cache,
      final Type superclass) {
    Class[] ctypes = cache instanceof LoadingCache ? new Class[]{LoadingCache.class, Cache.class }
        : new Class[]{Cache.class };
    List<TypeLiteral> result = new ArrayList<>(ctypes.length);
    for (Class ctype : ctypes) {
      if (name.equals("session")) {
        result.add(TypeLiteral.get(Types.newParameterizedType(ctype, String.class, Session.class)));
      } else {
        result.add(TypeLiteral.get(Types.newParameterizedType(ctype, types(superclass))));
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private String toSpec(final Object spec) {
    if (spec instanceof Map) {
      Map<String, Object> m = (Map<String, Object>) spec;
      return m.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining(","))
          .toString();
    }
    return spec.toString();
  }

  private static Type[] types(final Type superclass) {
    Type key = String.class;
    Type value = Object.class;
    if (superclass instanceof ParameterizedType) {
      ParameterizedType parameterized = (ParameterizedType) superclass;
      Type[] args = parameterized.getActualTypeArguments();
      key = args[0];
      value = args[1];
    }
    return new Type[]{key, value };
  }

}
