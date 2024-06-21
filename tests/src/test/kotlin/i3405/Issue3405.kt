/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3405

import io.jooby.StatusCode
import io.jooby.exception.StatusCodeException
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3405 {
  @ServerTest
  fun coroutineShouldFallbackToNormalErrorHandler(runner: ServerTestRunner) =
    runner
      .use {
        Kooby {
          error { ctx, cause, code -> ctx.send("normal/global") }
          coroutine { get("/i3405/normal-error") { ctx.query("q").value() } }
        }
      }
      .ready { client ->
        client.get("/i3405/normal-error") { rsp ->
          assertEquals("normal/global", rsp.body!!.string())
        }
      }

  @ServerTest
  fun coroutineSuspendErrorHandler(runner: ServerTestRunner) =
    runner
      .use {
        Kooby {
          error { ctx, cause, code -> ctx.send("normal/global") }
          coroutine {
            get("/i3405/coroutine-error") {
              delay(10)
              ctx.query("q").value()
            }

            error {
              delay(10)
              ctx.send("coroutine/suspended")
            }
          }
        }
      }
      .ready { client ->
        client.get("/i3405/coroutine-error") { rsp ->
          assertEquals("coroutine/suspended", rsp.body!!.string())
        }
      }

  @ServerTest
  fun coroutineChainSuspendErrorHandler(runner: ServerTestRunner) =
    runner
      .use {
        Kooby {
          error { ctx, cause, code -> ctx.send("normal/global") }
          coroutine {
            get("/i3405/chain") {
              if (ctx.query("q").isMissing) throw StatusCodeException(StatusCode.CONFLICT)
            }
            get("/i3405/coroutine-error") { ctx.query("q").value() }

            error(StatusCode.CONFLICT) { ctx.send("conflict") }
            error { ctx.send("coroutine/suspended") }
          }
        }
      }
      .ready { client ->
        client.get("/i3405/coroutine-error") { rsp ->
          assertEquals("coroutine/suspended", rsp.body!!.string())
        }
        client.get("/i3405/chain") { rsp -> assertEquals("conflict", rsp.body!!.string()) }
      }
}
