package io.jooby.utow;

import io.jooby.Context;
import io.jooby.Mode;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.internal.utow.UtowHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import org.xnio.Options;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private Mode mode = Mode.WORKER;

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public Server mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  @Nonnull @Override public Server tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  @Override public Server start(Router router) {
    HttpHandler uhandler = new UtowHandler(router, tmpdir);
    if (mode == Mode.WORKER) {
      uhandler = new BlockingHandler(uhandler);
    }
    server = Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setBufferSize(Context._16KB)
        // HTTP/1.1 is keep-alive by default
        .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
        .setServerOption(UndertowOptions.ALWAYS_SET_DATE, false)
        .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
        .setServerOption(UndertowOptions.DECODE_URL, false)
        .setHandler(uhandler)
        .build();

    server.start();

    return this;
  }

  @Override public Server stop() {
    server.stop();
    return this;
  }
}
