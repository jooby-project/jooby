/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package boot

import io.jooby.ExecutionMode
import io.jooby.ServerOptions
import io.jooby.jetty.JettyServer
import io.jooby.kt.Kooby
import io.jooby.kt.runApp
import io.jooby.netty.NettyServer
import io.jooby.undertow.UndertowServer

/** Just make sure all possible variables works. */
class RunApp : Kooby() {
  fun main(args: Array<String>) {
    val serverOptions = ServerOptions()
    // Single Provider
    runApp(args, ::RunApp)
    runApp(args, ExecutionMode.EVENT_LOOP, ::RunApp)
    runApp(args, NettyServer(), ::RunApp)
    runApp(args, NettyServer(serverOptions), ExecutionMode.EVENT_LOOP, ::RunApp)
    // Single Consumer
    runApp(args) { app -> }
    runApp(args, ExecutionMode.EVENT_LOOP) { app -> }
    runApp(args, JettyServer()) { app -> }
    runApp(args, JettyServer(serverOptions), ExecutionMode.EVENT_LOOP) { app -> }
    // Multiple
    runApp(args, ::RunApp, ::RunApp)
    runApp(args, ExecutionMode.EVENT_LOOP, ::RunApp, ::RunApp)
    runApp(args, UndertowServer(), ::RunApp, ::RunApp)
    runApp(args, UndertowServer(serverOptions), ExecutionMode.EVENT_LOOP, ::RunApp, ::RunApp)
  }
}
