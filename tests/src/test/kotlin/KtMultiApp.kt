/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import examples.multiapp.BarApp
import examples.multiapp.FooApp
import io.jooby.ExecutionMode
import io.jooby.kt.runApp
import io.jooby.netty.NettyServer

fun main(args: Array<String>) {
  runApp(args, NettyServer(), ::FooApp)

  runApp(args, NettyServer(), ExecutionMode.DEFAULT, ::BarApp, ::FooApp)

  runApp(args, NettyServer(), ::BarApp, ::FooApp)

  runApp(args, ::BarApp, ::FooApp)
}
