package i2598

import io.jooby.Context
import io.jooby.internal.RouteAnalyzer
import io.jooby.internal.asm.ClassSource
import io.jooby.jetty.Jetty
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import org.junit.jupiter.api.Assertions.assertEquals

class Issue2598 {

  @ServerTest(server = [Jetty::class])
  fun analyzerShouldDetectCompletableFuture(runner: ServerTestRunner) {
    val analyzer = RouteAnalyzer(ClassSource(javaClass.classLoader), false)

    val app = App2598()
    runner.use {
      app
    }.ready { _ ->
      val router = app.router
      val route = router.routes[0]
      val type = analyzer.returnType(route.handle)

      assertEquals(Context::class.java, type)
    }
  }

}

