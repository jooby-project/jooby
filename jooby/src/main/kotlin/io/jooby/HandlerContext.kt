package io.jooby

class AfterContext(val ctx: Context, val result: Any)

class DecoratorContext(val ctx: Context, val next: Route.Handler)

class HandlerContext (val ctx: Context)
