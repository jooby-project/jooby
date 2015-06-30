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
package org.jooby.memcached;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.compat.log.SLF4JLogger;
import net.spy.memcached.metrics.MetricType;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.internal.memcached.MemcachedClientProvider;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>memcached module</h1>
 * <p>
 * Provides memcached access via <a
 * href="https://github.com/dustin/java-memcached-client">SpyMemcached</a>
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>A {@link MemcachedClient} service</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>
 *  memcached.server = "localhost:11211"
 * </pre>
 *
 * <pre>
 * {
 *   use(new SpyMemcached());
 *
 *   get("/", req {@literal ->} {
 *     MemcachedClient client = req.require(MemcachedClient.class);
 *     client.set("foo", 60, "bar");
 *     return client.get("foo");
 *   });
 * }
 * </pre>
 *
 * <h2>configuration</h2>
 * <p>
 * It is done via <code>.conf</code> file:
 * </p>
 *
 * <pre>
 * memcached.protocol = binary
 * </pre>
 *
 * <p>
 * or programmatically:
 * </p>
 *
 * <pre>
 * {
 *   use(new SpyMemcached()
 *    .doWith(builder {@literal ->} {
 *      builder.setProtocol(Protocol.BINARY);
 *    })
 *  );
 * }
 * </pre>
 *
 * <p>
 * This module comes with a {@link Session.Store} too. See {@link SpySessionStore}.
 * </p>
 *
 * @author edgar
 * @since 0.7.0
 */
public class SpyMemcached implements Jooby.Module {

  static {
    System.setProperty("net.spy.log.LoggerImpl", SLF4JLogger.class.getName());
  }

  private BiConsumer<ConnectionFactoryBuilder, Config> configurer;

  /**
   * Creates a new {@link SpyMemcached} module.
   */
  public SpyMemcached() {
  }

  @SuppressWarnings("unchecked")
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Config $memcached = conf.getConfig("memcached")
        .withFallback(conf.getConfig("memcached"));

    ConnectionFactoryBuilder builder = newConnectionFactoryBuilder($memcached);

    if (configurer != null) {
      configurer.accept(builder, conf);
    }

    List<String> servers = new ArrayList<>();
    Object $servers = conf.getAnyRef("memcached.server");
    if ($servers instanceof List) {
      servers.addAll((Collection<? extends String>) $servers);
    } else {
      servers.add($servers.toString());
    }

    binder
        .bind(MemcachedClient.class)
        .toProvider(
            new MemcachedClientProvider(
                builder,
                AddrUtil.getAddresses(servers),
                $memcached.getDuration("shutdownTimeout", TimeUnit.MILLISECONDS)
            )
        )
        .asEagerSingleton();
  }

  /**
   * Configure a {@link ConnectionFactoryBuilder} programmatically.
   *
   * @param configurer A configure callback.
   * @return This module.
   */
  public SpyMemcached doWith(final BiConsumer<ConnectionFactoryBuilder, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  /**
   * Configure a {@link ConnectionFactoryBuilder} programmatically.
   *
   * @param configurer A configure callback.
   * @return This module.
   */
  public SpyMemcached doWith(final Consumer<ConnectionFactoryBuilder> configurer) {
    requireNonNull(configurer, "Configurer callback is required.");
    return doWith((b, c) -> configurer.accept(b));
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "memcached.conf");
  }

  private ConnectionFactoryBuilder newConnectionFactoryBuilder(final Config conf) {
    ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder();

    ifset(conf, "authWaitTime", path -> builder
        .setAuthWaitTime(conf.getDuration(path, TimeUnit.MILLISECONDS)));
    ifset(conf, "daemon", path -> builder.setDaemon(conf.getBoolean(path)));
    ifset(conf, "enableMetrics", path -> builder
        .setEnableMetrics(enumFor(conf.getString(path), MetricType.values())));
    ifset(conf, "failureMode", path -> builder
        .setFailureMode(enumFor(conf.getString(path), FailureMode.values())));
    ifset(conf, "locator", path -> builder
        .setLocatorType(enumFor(conf.getString(path), Locator.values())));
    ifset(conf, "maxReconnectDelay", path -> builder
        .setMaxReconnectDelay(conf.getDuration(path, TimeUnit.SECONDS)));
    ifset(conf, "opQueueMaxBlockTime", path -> builder
        .setOpQueueMaxBlockTime(conf.getDuration(path, TimeUnit.MILLISECONDS)));
    ifset(conf, "opTimeout", path -> builder
        .setOpTimeout(conf.getDuration(path, TimeUnit.MILLISECONDS)));
    ifset(conf, "protocol", path -> builder
        .setProtocol(enumFor(conf.getString(path), Protocol.values())));
    ifset(conf, "readBufferSize", path -> builder
        .setReadBufferSize(conf.getInt(path)));
    ifset(conf, "shouldOptimize", path -> builder
        .setShouldOptimize(conf.getBoolean(path)));
    ifset(conf, "timeoutExceptionThreshold", path -> builder
        .setTimeoutExceptionThreshold(conf.getInt(path)));
    ifset(conf, "useNagleAlgorithm", path -> builder
        .setUseNagleAlgorithm(conf.getBoolean(path)));
    return builder;
  }

  private void ifset(final Config conf, final String path, final Consumer<String> setter) {
    if (conf.hasPath(path)) {
      setter.accept(path);
    }
  }

  private <E extends Enum<E>> E enumFor(final String value, final E[] values) {
    for (E it : values) {
      if (it.name().equalsIgnoreCase(value)) {
        return it;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + value);
  }

}
