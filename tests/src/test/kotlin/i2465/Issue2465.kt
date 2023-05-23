/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2465

import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions

class Issue2465 {
  @ServerTest
  fun shouldCoroutineInvokeAfter(runner: ServerTestRunner) =
    runner
      .use {
        Kooby { ->
          after { ctx, value, _ ->
            ctx.setResponseHeader("After", value.toString())
            ctx.setResponseHeader("Response-Started", ctx.isResponseStarted)
          }
          coroutine {
            mvc(C2465())

            get("/2465") {
              delay(100)
              ctx.requestPath
            }
          }
        }
      }
      .ready { client ->
        client.get("/2465") { rsp ->
          Assertions.assertEquals("/2465", rsp.header("After"))
          Assertions.assertEquals("false", rsp.header("Response-Started"))
          Assertions.assertEquals("/2465", rsp.body!!.string())
        }

        client.get("/fun/2465") { rsp ->
          Assertions.assertEquals("/fun/2465", rsp.header("After"))
          Assertions.assertEquals("false", rsp.header("Response-Started"))
          Assertions.assertEquals("/fun/2465", rsp.body!!.string())
        }
      }
}
