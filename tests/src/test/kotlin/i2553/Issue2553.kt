package i2553

import io.jooby.internal.RouteAnalyzer
import io.jooby.internal.asm.ClassSource
import io.jooby.jetty.Jetty
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.CompletableFuture

class Issue2553 {

  @ServerTest(server = [Jetty::class])
  fun analyzerShouldDetectCompletableFuture(runner: ServerTestRunner) {
    val analyzer = RouteAnalyzer(ClassSource(javaClass.classLoader), false)

    val app = App2553()
    runner.use {
      app
    }.ready { http ->
      val router = app.router
      val route = router.routes.get(0)
      val type = analyzer.returnType(route.handle)

      assertEquals(CompletableFuture::class.java, type)
    }
  }

  @ServerTest
  fun shouldDetectCompletableFuture(runner: ServerTestRunner) {
    runner.use {
      App2553()
    }.ready { http ->
      http.get("/2553/var") { rsp ->
        assertEquals("var", rsp.body!!.string())
      }

      http.get("/2553/line") { rsp ->
        assertEquals("line", rsp.body!!.string())
      }
    }
  }

}

