/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2553

import io.jooby.internal.RouteAnalyzer
import io.jooby.internal.asm.ClassSource
import io.jooby.jetty.JettyServer
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.Assertions.assertEquals

class Issue2553 {

  @ServerTest(server = [JettyServer::class])
  fun analyzerShouldDetectCompletableFuture(runner: ServerTestRunner) {
    val analyzer = RouteAnalyzer(ClassSource(javaClass.classLoader), false)

    val app = App2553()
    runner
      .use { app }
      .ready { _ ->
        val router = app.router
        val route = router.routes[0]
        val type = analyzer.returnType(route.handle)

        assertEquals(CompletableFuture::class.java, type)
      }
  }

  @ServerTest
  fun shouldDetectCompletableFuture(runner: ServerTestRunner) {
    runner
      .use { App2553() }
      .ready { http ->
        http.get("/2553/var") { rsp -> assertEquals("var", rsp.body!!.string()) }

        http.get("/2553/line") { rsp -> assertEquals("line", rsp.body!!.string()) }
      }
  }
}
