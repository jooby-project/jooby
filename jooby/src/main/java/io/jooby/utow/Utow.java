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

import io.jooby.App;
import io.jooby.Functions;
import io.jooby.Server;
import io.jooby.internal.utow.UtowHandler;
import io.jooby.internal.utow.UtowMultiHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import org.xnio.Options;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private List<App> applications = new ArrayList<>();

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public Server deploy(App application) {
    applications.add(application);
    return this;
  }

  @Override public Server start() {
    Map<App, UtowHandler> handlers = new LinkedHashMap<>(applications.size());
    applications.forEach(app -> handlers.put(app, new UtowHandler(app)));

    HttpHandler handler = handlers.size() == 1
        ? handlers.values().iterator().next()
        : new UtowMultiHandler(handlers);

    server = Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setBufferSize(_16KB)
        // HTTP/1.1 is keep-alive by default, turn this option off
        .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
        .setServerOption(Options.BACKLOG, 10000)
        .setServerOption(Options.REUSE_ADDRESSES, true)
        .setServerOption(UndertowOptions.ALWAYS_SET_DATE, false)
        .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
        .setServerOption(UndertowOptions.DECODE_URL, false)
        .setHandler(handler)
        .build();

    server.start();

    applications.forEach(app -> {
      app.worker(Optional.ofNullable(app.worker()).orElse(server.getWorker()));
      app.start(this);
    });

    return this;
  }

  @Override public Server stop() {
    try (Functions.Closer closer = Functions.closer()) {
      applications.forEach(app -> closer.register(app::stop));
      closer.register(server::stop);
    }
    return this;
  }

}
