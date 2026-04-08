/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3477

import io.jooby.jackson.Jackson2Module
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3477 {
  @ServerTest
  fun shouldGenerateGoodKotlinRouteMetadata(runner: ServerTestRunner) =
    runner
      .use {
        Kooby {
          install(Jackson2Module())
          mvc(C3477_())
        }
      }
      .ready { client ->
        client.get("/3477") { rsp ->
          assertEquals("{\"Transactional\":false}", rsp.body!!.string())
        }
      }
}
