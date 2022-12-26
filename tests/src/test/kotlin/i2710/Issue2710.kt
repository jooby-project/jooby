/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2710

import io.jooby.AccessLogHandler
import io.jooby.Kooby
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import java.util.concurrent.CountDownLatch
import org.junit.jupiter.api.Assertions

class Issue2710 {
  @ServerTest
  fun shouldLogLine(runner: ServerTestRunner) {
    val latch = CountDownLatch(1)
    runner
      .use {
        Kooby { ->
          use(
            AccessLogHandler().log { line ->
              Assertions.assertTrue(line.contains("\"GET /2710 HTTP/1.1\" 200 2"))
              log.info(line)
              latch.countDown()
            }
          )

          coroutine { get("/2710") { "OK" } }
        }
      }
      .ready { client ->
        client.get("/2710") { rsp -> Assertions.assertEquals("OK", rsp.body!!.string()) }
      }
    latch.await()
  }
}
