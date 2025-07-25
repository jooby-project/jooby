/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.boot;

import java.util.List;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.netty.NettyServer;
import io.jooby.undertow.UndertowServer;

/** Just make sure all possible variables works. */
public class RunApp extends Jooby {

  public static void main(String[] args) {
    var serverOptions = new ServerOptions();
    // Single Provider
    runApp(args, RunApp::new);
    runApp(args, ExecutionMode.EVENT_LOOP, RunApp::new);
    runApp(args, new NettyServer(), RunApp::new);
    runApp(args, new NettyServer(serverOptions), ExecutionMode.EVENT_LOOP, RunApp::new);
    // Single Consumer
    runApp(args, app -> {});
    runApp(args, ExecutionMode.EVENT_LOOP, app -> {});
    runApp(args, new JettyServer(), app -> {});
    runApp(args, new JettyServer(serverOptions), ExecutionMode.EVENT_LOOP, app -> {});
    // Multiple
    runApp(args, List.of(RunApp::new, RunApp::new));
    runApp(args, ExecutionMode.EVENT_LOOP, List.of(RunApp::new, RunApp::new));
    runApp(args, new UndertowServer(), List.of(RunApp::new, RunApp::new));
    runApp(
        args,
        new UndertowServer(serverOptions),
        ExecutionMode.EVENT_LOOP,
        List.of(RunApp::new, RunApp::new));
  }
}
