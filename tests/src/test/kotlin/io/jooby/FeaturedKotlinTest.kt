package io.jooby

import io.jooby.internal.mvc.KotlinMvc
import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals

class FeaturedKotlinTest {

  @ServerTest
  fun explicitContext(runner: ServerTestRunner) {
    runner.define { app ->
      app.get("/") {
        "Hello World!"
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("Hello World!", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun implicitContext(runner: ServerTestRunner) {
    runner.use { ->
      Kooby {
        get("/") {
          ctx.send("Hello World!")
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("Hello World!", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun coroutineNoSuspend(runner: ServerTestRunner) {
    runner.use { ->
      Kooby {
        coroutine {
          get("/") {
            ctx.getRequestPath() + "coroutine"
          }
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun coroutineSuspend(runner: ServerTestRunner) {
    runner.use { ->
      Kooby {
        coroutine {
          get("/") {
            delay(100)
            ctx.getRequestPath() + "coroutine"
          }
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun javaApiWithReactiveType(runner: ServerTestRunner) {
    runner.use { ->
      Jooby().apply {
        get("/") {
          println(Thread.currentThread())
          Flowable.range(1, 10)
              .map { i -> i.toString() + "," }
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun mvc(runner: ServerTestRunner) {
    runner.define { app ->
      app.mvc(KotlinMvc())
    }.ready { client ->
      client.get("/kotlin") { rsp ->
        assertEquals("Got it!", rsp.body!!.string())
      }

      client.get("/kotlin/78") { rsp ->
        assertEquals("78", rsp.body!!.string())
      }

      client.get("/kotlin/point?x=8&y=1") { rsp ->
        assertEquals("QueryPoint(x=8, y=1) : 8", rsp.body!!.string())
      }

      client.get("/kotlin/point") { rsp ->
        assertEquals("QueryPoint(x=null, y=null) : null", rsp.body!!.string())
      }

      client.get("/kotlin/point?x=9") { rsp ->
        assertEquals("QueryPoint(x=9, y=null) : 9", rsp.body!!.string())
      }

      client.get("/kotlin/point?x=9&y=8") { rsp ->
        assertEquals("QueryPoint(x=9, y=8) : 9", rsp.body!!.string())
      }
    }
  }

  @ServerTest
  fun suspendMvc(runner: ServerTestRunner) {
    runner.use { ->
      Kooby {
        coroutine {
          mvc(SuspendMvc())
        }

        error { ctx, cause, statusCode ->
          log.error("{} {}", ctx.method, ctx.getRequestPath(), cause)
          ctx.setResponseCode(statusCode)
              .send(cause.message!!)
        }
      }
    }.ready ({ client ->
      client.get("/") { rsp ->
        assertEquals("Got it!", rsp.body!!.string())
      }

      client.get("/delay") { rsp ->
        assertEquals("/delay", rsp.body!!.string())
      }

      client.get("/456") { rsp ->
        assertEquals("456", rsp.body!!.string())
      }

      client.get("/456x") { rsp ->
        assertEquals("Cannot convert value: 'id', to: 'int'", rsp.body!!.string())
      }
    })
  }
}
