package io.jooby

import io.jooby.internal.mvc.KotlinMvc
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeaturedKotlinTest {

  @Test
  fun explicitContext() {
    JoobyRunner { app ->
      app.get("/") {
        "Hello World!"
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("Hello World!", rsp.body!!.string())
      }
    }
  }

  @Test
  fun implicitContext() {
    JoobyRunner { ->
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

  @Test
  fun coroutineNoSuspend() {
    JoobyRunner { ->
      Kooby {
        coroutine {
          get {
            ctx.pathString() + "coroutine"
          }
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body!!.string())
      }
    }
  }

  @Test
  fun coroutineSuspend() {
    JoobyRunner { ->
      Kooby {
        coroutine {
          get("/") {
            delay(100)
            ctx.pathString() + "coroutine"
          }
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body!!.string())
      }
    }
  }

  @Test
  fun javaApiWithReactiveType() {
    JoobyRunner { ->
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

  fun mvc() {
    JoobyRunner { app ->
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

  fun suspendMvc() {
    JoobyRunner { ->
      Kooby {
        coroutine {
          mvc(SuspendMvc())
        }

        error { ctx, cause, statusCode ->
          log.error("{} {}", ctx.method, ctx.pathString(), cause)
          ctx.setResponseCode(statusCode)
              .send(cause.message!!)
        }
      }
    }.ready { client ->
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
        assertEquals("Unable to provision parameter: 'id: int'", rsp.body!!.string())
      }
    }
  }
}
