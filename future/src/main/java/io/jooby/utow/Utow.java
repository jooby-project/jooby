package io.jooby.utow;

import io.jooby.Handler;
import io.jooby.Mode;
import io.jooby.RootHandler;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.internal.utow.UtowContext;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import org.xnio.XnioWorker;

import java.util.concurrent.atomic.AtomicReference;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private Mode mode = Mode.WORKER;

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public Server mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  @Override public Server start(Router router) {
    AtomicReference<XnioWorker> ref = new AtomicReference<>();
    Undertow.Builder builder = Undertow.builder()
        .addHttpListener(port, "0.0.0.0");
    HttpHandler uhandler = exchange -> {
      RootHandler handler = router
          .matchRoot(exchange.getRequestMethod().toString(), exchange.getRequestPath());
      handler.apply(new UtowContext(exchange, ref.get()));
    };
    if (mode == Mode.WORKER) {
      uhandler = new BlockingHandler(uhandler);
    }
    builder.setHandler(uhandler).build();

    server = builder.build();
    ref.set(server.getWorker());

    server.start();

    return this;
  }

  @Override public Server stop() {
    server.stop();
    return this;
  }
}
