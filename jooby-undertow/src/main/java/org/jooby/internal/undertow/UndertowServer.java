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
package org.jooby.internal.undertow;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Option;
import org.xnio.Options;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

public class UndertowServer implements org.jooby.spi.Server {

  private interface SetOption {
    void set(String name);
  }

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(org.jooby.spi.Server.class);

  private Undertow server;

  private final GracefulShutdownHandler shutdown;

  private long awaitShutdown;

  @Inject
  public UndertowServer(final org.jooby.spi.HttpHandler dispatcher, final Config config,
      final Provider<SSLContext> sslContext)
          throws Exception {

    awaitShutdown = config.getDuration("undertow.awaitShutdown", TimeUnit.MILLISECONDS);
    shutdown = new GracefulShutdownHandler(doHandler(dispatcher, config));
    Undertow.Builder ubuilder = configure(config, io.undertow.Undertow.builder())
        .addHttpListener(config.getInt("application.port"),
            host(config.getString("application.host")));

    if (config.hasPath("application.securePort")) {
      ubuilder.addHttpsListener(config.getInt("application.securePort"),
          host(config.getString("application.host")), sslContext.get());
    }

    this.server = ubuilder.setHandler(shutdown)
        .build();
  }

  private String host(final String host) {
    return "localhost".equals(host) ? "0.0.0.0" : host;
  }

  @SuppressWarnings("unchecked")
  static Builder configure(final Config config, final Builder builder) {
    Config $undertow = config.getConfig("undertow");

    set($undertow, "bufferSize", name -> {
      int value = $undertow.getBytes(name).intValue();
      log.debug("undertow.bufferSize({})", value);
      builder.setBufferSize(value);
    });

    set($undertow, "buffersPerRegion", name -> {
      int value = $undertow.getInt(name);
      log.debug("undertow.buffersPerRegion({})", value);
      builder.setBuffersPerRegion(value);
    });

    set($undertow, "directBuffers", name -> {
      boolean value = $undertow.getBoolean(name);
      log.debug("undertow.directBuffers({})", value);
      builder.setDirectBuffers(value);
    });

    set($undertow, "ioThreads", name -> {
      int value = $undertow.getInt(name);
      log.debug("undertow.ioThreads({})", value);
      builder.setIoThreads(value);
    });

    set($undertow, "workerThreads", name -> {
      int value = $undertow.getInt(name);
      log.debug("undertow.workerThreads({})", value);
      builder.setWorkerThreads(value);
    });

    $undertow.getConfig("server").root().entrySet()
        .forEach(setOption($undertow, "server",
            (option, value) -> builder.setServerOption(option, value)));

    $undertow.getConfig("worker").root().entrySet()
        .forEach(setOption($undertow, "worker",
            (option, value) -> builder.setWorkerOption(option, value)));

    $undertow.getConfig("socket").root().entrySet()
        .forEach(setOption($undertow, "socket",
            (option, value) -> builder.setSocketOption(option, value)));

    return builder;
  }

  @SuppressWarnings("rawtypes")
  private static Consumer<Map.Entry<String, ConfigValue>> setOption(final Config config,
      final String level,
      final BiConsumer<Option, Object> setter) {
    return entry -> {
      String name = entry.getKey();
      Object value = entry.getValue().unwrapped();
      Option option = findOption(name, UndertowOptions.class, Options.class);
      if (option != null) {
        // parse option to adjust correct type
        Object parsedValue = value.toString();
        try {
          parsedValue = option.parseValue(value.toString(), null);
        } catch (NumberFormatException ex) {
          // try bytes
          try {
            parsedValue = option.parseValue(config.getBytes(level + "." + name).toString(), null);
          } catch (ConfigException.BadValue badvalue) {
            // try duration
            parsedValue = option.parseValue(
                config.getDuration(level + "." + name, TimeUnit.MILLISECONDS) + "", null);
          }
        }
        log.debug("{}.{}({})", level, option.getName(), parsedValue);
        setter.accept(option, parsedValue);
      } else {
        log.error("Unknown option: 'undertow.{}.{} = {}'", level, name, value);
      }
    };
  }

  @SuppressWarnings("rawtypes")
  private static Option findOption(final String name, final Class... where) {
    for (Class owner : where) {
      try {
        Field fld = owner.getDeclaredField(name);
        return (Option) fld.get(null);
      } catch (NoSuchFieldException | IllegalAccessException ex) {
        log.trace("Unknown option: '{}'", name);
      }
    }
    return null;
  }

  private static void set(final Config config, final String name, final SetOption setter) {
    if (config.hasPath(name)) {
      setter.set(name);
    }
  }

  private static HttpHandler doHandler(final org.jooby.spi.HttpHandler dispatcher,
      final Config config) {
    return new UndertowHandler(dispatcher, config);
  }

  @Override
  public void start() throws Exception {
    server.start();
  }

  @Override
  public void join() throws InterruptedException {
    // NOOP
  }

  @Override
  public void stop() throws Exception {
    shutdown.shutdown();
    shutdown.awaitShutdown(awaitShutdown);
    server.stop();
  }

}
