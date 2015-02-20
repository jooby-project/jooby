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

import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jooby.spi.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Option;
import org.xnio.Options;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class UndertowServer implements org.jooby.spi.Server {

  private interface SetOption {
    void set(String name);
  }

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(UndertowServer.class);

  private io.undertow.Undertow server;

  private final GracefulShutdownHandler shutdown;

  @Inject
  public UndertowServer(final Dispatcher dispatcher, final Config config) throws Exception {

    shutdown = new GracefulShutdownHandler(doHandler(dispatcher, config));
    this.server = configure(config, io.undertow.Undertow.builder())
        .addHttpListener(config.getInt("application.port"), config.getString("application.host"))
        .setHandler(shutdown)
        .build();
  }

  @SuppressWarnings("unchecked")
  static Builder configure(final Config config, final Builder builder) {
    Config $undertow = config.getConfig("undertow");

    set($undertow, "bufferSize", name ->
        builder.setBufferSize($undertow.getInt(name)));

    set($undertow, "buffersPerRegion", name ->
        builder.setBuffersPerRegion($undertow.getInt(name)));

    set($undertow, "directBuffers", name ->
        builder.setDirectBuffers($undertow.getBoolean(name)));

    set($undertow, "ioThreads", name ->
        builder.setIoThreads($undertow.getInt(name)));

    set($undertow, "workerThreads", name ->
        builder.setWorkerThreads($undertow.getInt(name)));

    $undertow.getConfig("server").root().entrySet()
        .forEach(setOption("server", (option, value) -> builder.setServerOption(option, value)));

    $undertow.getConfig("worker").root().entrySet()
        .forEach(setOption("worker", (option, value) -> builder.setWorkerOption(option, value)));

    $undertow.getConfig("socket").root().entrySet()
        .forEach(setOption("socket", (option, value) -> builder.setSocketOption(option, value)));

    return builder;
  }

  @SuppressWarnings("rawtypes")
  private static Consumer<Map.Entry<String, ConfigValue>> setOption(final String level,
      final BiConsumer<Option, Object> setter) {
    return entry -> {
      String name = entry.getKey();
      Object value = entry.getValue().unwrapped();
      Option option = findOption(name, UndertowOptions.class, Options.class);
      if (option != null) {
        // parse option to adjust correct type
        setter.accept(option, option.parseValue(value.toString(), null));
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

  private static HttpHandler doHandler(final Dispatcher dispatcher,
      final Config config) {
    String tmpdir = config.getString("application.tmpdir");
    Charset charset = Charset.forName(config.getString("application.charset"));
    return new UndertowHandler(dispatcher, tmpdir, charset);
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
    // TODO add a shutdown timeout
    shutdown.awaitShutdown();
    server.stop();
  }

}
