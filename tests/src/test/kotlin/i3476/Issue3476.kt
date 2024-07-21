/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3476

import io.jooby.jackson.JacksonModule
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3476 {
  @ServerTest
  fun shouldGenerateGoodKotlinRouteMetadata(runner: ServerTestRunner) =
    runner
      .use {
        Kooby {
          install(JacksonModule())
          mvc(C3476_())
        }
      }
      .ready { client -> client.get("/3476") { rsp -> assertEquals("[]", rsp.body!!.string()) } }
}
