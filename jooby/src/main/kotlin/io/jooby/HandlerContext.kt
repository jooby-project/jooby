/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby

class AfterContext(val ctx: Context, val result: Any?)

class DecoratorContext(val ctx: Context, val next: Route.Handler)

class HandlerContext (val ctx: Context)
