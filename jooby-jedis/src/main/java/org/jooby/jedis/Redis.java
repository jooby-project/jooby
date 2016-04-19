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
package org.jooby.jedis;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis cache and key/value data store for Jooby. Exposes a {@link Jedis} service.
 *
 * <h1>usage</h1>
 * <p>
 * It is pretty straightforward:
 * </p>
 *
 * <pre>
 * # define a database URI
 * db = "redis://localhost:6379"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Redis());
 *
 *    get("/:key/:value", req {@literal ->} {
 *      try (Jedis jedis = req.require(Jedis.class)) {
 *        jedis.set(req.param("key").value(), req.param("value").value());
 *        return jedis.get(req.param("key").value());
 *      }
 *    });
 * }
 * </pre>
 *
 * <h1>pool configuration</h1>
 * <p>
 * This module creates a {@link JedisPool}. A default pool is created with a max of <code>128</code>
 * instances.
 * </p>
 *
 * <p>
 * The pool can be customized from your <code>application.conf</code>:
 * </p>
 *
 * <pre>
 * db = "redis://localhost:6379"
 *
 * # increase pool size to 200
 * jedis.pool.maxTotal = 200
 * </pre>
 *
 * <h2>two or more redis connections</h2>
 * <p>
 * In case you need two or more Redis connection, just do:
 * </p>
 *
 * <pre>
 * {
 *   use(new Redis()); // default is "db"
 *   use(new Redis("db1"));
 *
 *   get("/:key/:value", req {@literal ->} {
 *     try (Jedis jedis = req.require("db1", Jedis.class)) {
 *       jedis.set(req.param("key").value(), req.param("value").value());
 *       return jedis.get(req.param("key").value());
 *     }
 *   });
 * }
 * </pre>
 *
 * application.conf:
 *
 * <pre>
 * db = "redis://localhost:6379/0"
 *
 * db1 = "redis://localhost:6379/1"
 * </pre>
 *
 * Pool configuration for <code>db1</code> is inherited from <code>jedis.pool</code>. If you need
 * to tweak the pool configuration for <code>db1</code> just do:
 *
 * <pre>
 * db1 = "redis://localhost:6379/1"
 *
 * # ONLY 10 for db1
 * jedis.db1.maxTotal = 10
 * </pre>
 *
 * <p>
 * For more information about <a href="https://github.com/xetorthio/jedis">Jedis</a> checkout the <a
 * href="https://github.com/xetorthio/jedis/wiki">wiki</a>
 * </p>
 *
 * <p>
 * That's all folks! Enjoy it!
 * </p>
 *
 * TBD: Object mapping? https://github.com/xetorthio/johm?
 *
 * @author edgar
 * @since 0.5.0
 */
public class Redis implements Jooby.Module {

  /**
   * Database name.
   */
  private String name;

  /**
   * True, if the Guice binding require a name.
   */
  private boolean named;

  /**
   * <p>
   * Creates a new {@link Redis} instance and connect to the provided database. Please note, the
   * name is a property in your <code>application.conf</code> file with Redis URI.
   * </p>
   *
   * <pre>
   * {
   *   use(new Redis("db1"));
   * }
   * </pre>
   *
   * application.conf
   *
   * <pre>
   * db1 = ""redis://localhost:6379""
   * </pre>
   *
   * Default database name is: <code>db</code>
   *
   * @param name A database name.
   */
  public Redis(final String name) {
    this.name = requireNonNull(name, "A db property is required.");
    this.named = !"db".equals(name);
  }

  /**
   * A new {@link Redis} instance, with a default database name of: <code>db</code>.
   */
  public Redis() {
    this("db");
  }

  /**
   * Bind {@link JedisPool} and {@link Jedis} services with a {@link Named} annotation.
   *
   * @return This module.
   */
  public Redis named() {
    this.named = true;
    return this;
  }

  /**
   * Bind {@link JedisPool} and {@link Jedis} services without a {@link Named} annotation.
   *
   * @return This module.
   */
  public Redis unnamed() {
    this.named = false;
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    /**
     * Pool
     */
    GenericObjectPoolConfig poolConfig = poolConfig(config, name);
    int timeout = (int) config.getDuration("jedis.timeout", TimeUnit.MILLISECONDS);
    URI uri = URI.create(config.getString(name));
    JedisPool pool = new JedisPool(poolConfig, uri, timeout);

    RedisProvider provider = new RedisProvider(pool, uri, poolConfig);
    env.onStart(provider::start);
    env.onStop(provider::stop);

    Provider<Jedis> jedis = (Provider<Jedis>) () -> pool.getResource();

    /**
     * Guice
     */
    if (named) {
      binder.bind(JedisPool.class)
          .annotatedWith(Names.named(name))
          .toInstance(pool);

      binder.bind(Jedis.class).annotatedWith(Names.named(name)).toProvider(jedis)
          .asEagerSingleton();
    } else {
      binder.bind(JedisPool.class).toInstance(pool);

      binder.bind(Jedis.class).toProvider(jedis)
          .asEagerSingleton();
    }
  }

  private GenericObjectPoolConfig poolConfig(final Config config, final String name) {
    Config poolConfig = config.getConfig("jedis.pool");
    String override = "jedis." + name;
    if (config.hasPath(override)) {
      poolConfig = config.getConfig(override).withFallback(poolConfig);
    }
    return poolConfig(poolConfig);
  }

  private GenericObjectPoolConfig poolConfig(final Config config) {
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setBlockWhenExhausted(config.getBoolean("blockWhenExhausted"));
    poolConfig.setEvictionPolicyClassName(config.getString("evictionPolicyClassName"));
    poolConfig.setJmxEnabled(config.getBoolean("jmxEnabled"));
    poolConfig.setJmxNamePrefix(config.getString("jmxNamePrefix"));
    poolConfig.setLifo(config.getBoolean("lifo"));
    poolConfig.setMaxIdle(config.getInt("maxIdle"));
    poolConfig.setMaxTotal(config.getInt("maxTotal"));
    poolConfig.setMaxWaitMillis(config.getDuration("maxWait", TimeUnit.MILLISECONDS));
    poolConfig.setMinEvictableIdleTimeMillis(config.getDuration("minEvictableIdle",
        TimeUnit.MILLISECONDS));
    poolConfig.setMinIdle(config.getInt("minIdle"));
    poolConfig.setNumTestsPerEvictionRun(config.getInt("numTestsPerEvictionRun"));
    poolConfig.setSoftMinEvictableIdleTimeMillis(
        config.getDuration("softMinEvictableIdle", TimeUnit.MILLISECONDS));
    poolConfig.setTestOnBorrow(config.getBoolean("testOnBorrow"));
    poolConfig.setTestOnReturn(config.getBoolean("testOnReturn"));
    poolConfig.setTestWhileIdle(config.getBoolean("testWhileIdle"));
    poolConfig.setTimeBetweenEvictionRunsMillis(config.getDuration("timeBetweenEvictionRuns",
        TimeUnit.MILLISECONDS));

    return poolConfig;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "jedis.conf");
  }

}
