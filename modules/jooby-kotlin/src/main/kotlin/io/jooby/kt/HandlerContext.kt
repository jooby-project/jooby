/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.Context
import io.jooby.Route
import io.jooby.ServerSentEmitter
import io.jooby.WebSocketConfigurer

class AfterContext(val ctx: Context, val result: Any?, val failure: Any?)

class FilterContext(val ctx: Context, val next: Route.Handler)

class HandlerContext(val ctx: Context) : java.io.Serializable

class WebSocketInitContext(val ctx: Context, val configurer: WebSocketConfigurer)

class ServerSentHandler(val ctx: Context, val sse: ServerSentEmitter)
