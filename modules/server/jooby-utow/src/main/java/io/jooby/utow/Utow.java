/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.utow;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.Throwing;
import io.jooby.internal.utow.UtowHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import org.xnio.Options;

import javax.annotation.Nonnull;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Utow extends Server.Base {

  private Undertow server;

  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options = new ServerOptions()
      .setIoThreads(ServerOptions.IO_THREADS)
      .setServer("utow");

  @Nonnull @Override public Utow setOptions(ServerOptions options) {
    this.options = options
        .setIoThreads(options.getIoThreads());
    return this;
  }

  @Nonnull @Override public ServerOptions getOptions() {
    return options;
  }

  @Override public Server start(Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      HttpHandler handler = new UtowHandler(applications.get(0), options.getBufferSize(),
          options.getMaxRequestSize(),
          options.isDefaultHeaders());

      if (options.isGzip()) {
        handler = new EncodingHandler.Builder().build(null).wrap(handler);
      }

      server = Undertow.builder()
          .addHttpListener(options.getPort(), "0.0.0.0")
          .setBufferSize(options.getBufferSize())
          /** Socket : */
          .setSocketOption(Options.BACKLOG, 8192)
          /** Server: */
          // HTTP/1.1 is keep-alive by default, turn this option off
          .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
          .setServerOption(UndertowOptions.ALWAYS_SET_DATE, options.isDefaultHeaders())
          .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
          .setServerOption(UndertowOptions.DECODE_URL, false)
          /** Worker: */
          .setIoThreads(options.getIoThreads())
          .setWorkerOption(Options.WORKER_NAME, "application")
          .setWorkerThreads(options.getWorkerThreads())
          .setHandler(handler)
          .build();

      server.start();
      // NOT IDEAL, but we need to fire onStart after server.start to get access to Worker
      fireStart(applications, server.getWorker());

      fireReady(Collections.singletonList(application));

      return this;
    } catch (RuntimeException x) {
      Throwable cause = Optional.ofNullable(x.getCause()).orElse(x);
      if (cause instanceof BindException) {
        cause = new BindException("Address already in use: " + options.getPort());
      }
      throw Throwing.sneakyThrow(cause);
    }
  }

  @Override public Server stop() {
    fireStop(applications);
    applications = null;
    if (server != null) {
      server.stop();
      server = null;
    }
    return this;
  }

}
