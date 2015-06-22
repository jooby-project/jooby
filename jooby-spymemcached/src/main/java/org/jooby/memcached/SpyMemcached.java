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
import org.jooby.internal.memcached.MemcachedClientProvider;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 *
 * @author edgar
 * @since 0.7.0
 */
public class SpyMemcached implements Jooby.Module {

  static {
    System.setProperty("net.spy.log.LoggerImpl", SLF4JLogger.class.getName());
  }

  private BiConsumer<ConnectionFactoryBuilder, Config> configurer;

  private final String name;

  public SpyMemcached(final String name) {
    this.name = requireNonNull(name, "Name property is required.");
  }

  public SpyMemcached() {
    this("memcached");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Config $memcached = conf.getConfig(name)
        .withFallback(conf.getConfig("memcached"));

    ConnectionFactoryBuilder builder = newConnectionFactoryBuilder($memcached);

    if (configurer != null) {
      configurer.accept(builder, conf);
    }

    List<String> servers = new ArrayList<>();
    Object $servers = conf.getAnyRef(name + ".server");
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

  public SpyMemcached doWith(final BiConsumer<ConnectionFactoryBuilder, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

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
